package com.team11.jojopay.domain.refund.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.entity.RefundItem;
import com.team11.jojopay.domain.refund.repository.RefundItemRepository;
import com.team11.jojopay.domain.refund.repository.RefundRepository;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final OrderValidator orderValidator;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final ProductRepository productRepository;

    private final PointService pointService;
    private final MemberService memberService;
    private final PortOneClient portOneClient;

    @Transactional
    public void refundOrder(Long memberId, RefundRequest request) {

        // 1. 주문 마스터 및 결제 내역 밸리데이터를 통해 안전하게 획득
        Order order = orderValidator.validateAndGetOrder(request.getOrderNumber(), memberId);
        Payment payment = orderValidator.validateAndGetPayment(order);
        Member member = memberService.findMemberById(order.getMemberId());

        // 💡 [팀 인프라 활용] 요청받은 orderItemId 목록을 추출하여 IN 쿼리로 통째로 유효성 및 소유권 검증!
        List<Long> requestItemIds = request.getItems().stream().map(RefundRequest.RefundItemRequest::getOrderItemId).toList();
        List<OrderItem> refundItems = orderValidator.validateAndGetOrderItems(requestItemIds, memberId);

        // 빠른 조회를 위해 클라이언트의 요청 수량을 Map (Key: orderItemId -> Value: 환불요청 수량) 구조로 파싱
        Map<Long, Integer> requestQuantityMap = request.getItems().stream().collect(Collectors.toMap(RefundRequest.RefundItemRequest::getOrderItemId, RefundRequest.RefundItemRequest::getQuantity));

        long totalRefundAmount = 0;
        List<RefundItem> readyRefundItems = new ArrayList<>();

        // 2. 서버단에서 환불 가능 조건 검증 및 금액 산정 (클라이언트 데이터 신뢰 X)
        for (OrderItem item : refundItems) {
            int requestQty = requestQuantityMap.get(item.getId());

            // 기존 누적 환불 완료 수량 조회
            int alreadyRefundedQty = refundItemRepository.sumQuantityByOrderItemId(item.getId());

            // 수량 검증: (기존 환불 수량 + 이번 요청 수량) > 원본 주문 수량이면 예외 발생
            if (alreadyRefundedQty + requestQty > item.getQuantity()) {
                throw new ServiceException(ErrorCode.EXCEEDED_REFUND_QUANTITY);
            }

            // 결제 당시의 스냅샷 가격(priceAtOrder)을 기준으로 환불액 계산 및 합산
            totalRefundAmount += (item.getPriceAtOrder() * requestQty);

            // [도메인 규칙 자율화] Product 엔티티 직접 조회 및 내부 메서드로 재고 증가 복구
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
            product.increaseStock(requestQty); // JPA 더티 체킹 작동

            // 영수증에 박을 자식 엔티티 조립
            readyRefundItems.add(RefundItem.createRefundItem(item.getId(), requestQty));
        }

        // 3. 도메인 정산 계산기 가동 (int 등급 적립률 원본 전달)
        long currentPointBalance = member.getPointBalance();
        RefundCalculator calculator = new RefundCalculator(
                totalRefundAmount,
                payment.getAmount(),
                payment.getUsedPoint(),
                payment.getPgRealAmount(),
                member.getMembershipGrade().getRewardRate(),
                currentPointBalance
        );

        // 4. [포인트 도메인 원장 통합 규격] 적용 (사용분 복구 및 적립분 몰수)
        if (calculator.getFinalPointRestoreAmount() > 0) {
            pointService.createHistory(member, payment, PointTransactionType.USE_RECOVERY, calculator.getFinalPointRestoreAmount());
        }

        long actualPointToDeduct = Math.min(currentPointBalance, calculator.getPointToRecoverFromEarn());
        if (actualPointToDeduct > 0) {
            pointService.createHistory(member, payment, PointTransactionType.EARN_FORFEIT, actualPointToDeduct);
        }

        // 5. 환불 마스터 및 상세 영수증 테이블 최종 DB 영속화
        Refund refund = Refund.createRefund(
                order,
                request.getReason(),
                calculator.getTotalRefundAmount(),
                calculator.getFinalPointRestoreAmount(),
                calculator.getFinalPgCancelAmount()
        );

        for (RefundItem ri : readyRefundItems) {
            refund.addRefundItem(ri);
        }
        refundRepository.save(refund);

        // 6. 트랜잭션 최하단 최후의 보루: 외부 포트원 PG 결제 승인 취소 무전 송출
        if (calculator.getFinalPgCancelAmount() > 0) {
            try {
                portOneClient.cancelPayment(
                        payment.getPortonePaymentId(),
                        request.getReason(),
                        calculator.getFinalPgCancelAmount()
                );
                log.info("[포트원 PG 환불 성공] 거래키: {}, 취소액: {}원", payment.getPortonePaymentId(), calculator.getFinalPgCancelAmount());

            } catch (Exception e) {
                log.error("[포트원 통신 실패] DB 데이터는 저장되었으나 실결제 취소 실패", e);
                log.error("수동 정산 대상 가동 요망 -> 거래키: {}, 금액: {}", payment.getPortonePaymentId(), calculator.getFinalPgCancelAmount());

                throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }
        }
    }
}
