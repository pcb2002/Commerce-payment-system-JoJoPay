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

/**
 * 환불 처리의 전반적인 비즈니스 오케스트레이션을 관장하는 서비스 클래스입니다.
 * 주문 검증, 상품 재고 원상복구, 정산 금액 분할 계산, 포인트 원장 처리, 외부 PG 연동을 일괄 트랜잭션 내에서 제어합니다.
 */
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

    /**
     * DTO의 비즈니스 주문 번호를 기반으로 환불 오퍼레이션을 수행합니다.
     * 1. 밸리데이터 기반 주문 마스터 정보 검증 및 영속성 획득
     * 2. 복수 환불 상품 대상을 대용량 IN 쿼리 배치를 통해 일괄 소유권 검증 수행
     * 3. 잔여 환불 가능 수량 초과 여부 확인 및 도메인 모델 내부 메서드를 이용한 실시간 재고 복구
     * 4. 정산 계산기(`RefundCalculator`) 가동을 통한 복합 결제 비율 연산 및 체리피커 포인트 방어 처리
     * 5. 포인트 원장 통합 규격에 맞춘 포인트 사용분 복구 및 적립분 몰수 이력 적재
     * 6. 포트원(PortOne) API 외부 원격 무전을 통한 PG 승인 금액 정밀 부분/전액 취소 전송
     *
     * @param memberId 환불을 요청한 주체의 회원 고유 식별자 ID
     * @param request  주문 번호 및 환불 상세 아이템 목록을 바인딩한 요청 DTO 객체
     * @throws ServiceException 잔여 환불 수량 초과, 상품 부재, 외부 결제 대행사 취소 통신 에러 시 발생
     */
    @Transactional
    public void refundOrder(Long memberId, RefundRequest request) {

        // 1. DTO에 담긴 비즈니스 주문번호(orderNumber)를 꺼내서 주문 마스터 영속성 안전하게 획득
        Order order = orderValidator.validateAndGetOrder(request.getOrderNumber(), memberId);
        Payment payment = orderValidator.validateAndGetPayment(order);
        Member member = memberService.findMemberById(order.getMemberId());

        // 요청받은 orderItemId 목록을 추출하여 IN 쿼리로 통째로 유효성 및 소유권 검증!
        List<Long> requestItemIds = request.getItems().stream().map(RefundRequest.RefundItemRequest::getOrderItemId).toList();
        List<OrderItem> refundItems = orderValidator.validateAndGetOrderItems(requestItemIds, memberId);

        // 연산 최적화를 위해 클라이언트의 요청 수량을 Map (Key: orderItemId -> Value: 환불요청 수량) 구조로 가공
        Map<Long, Integer> requestQuantityMap = request.getItems().stream().collect(Collectors.toMap(RefundRequest.RefundItemRequest::getOrderItemId, RefundRequest.RefundItemRequest::getQuantity));

        long totalRefundAmount = 0;
        List<RefundItem> readyRefundItems = new ArrayList<>();

        // 2. 서버단 독립 검증 체계를 이용한 단가 산정 및 수량 무결성 체킹
        for (OrderItem item : refundItems) {
            int requestQty = requestQuantityMap.get(item.getId());

            // 데이터베이스 내 누적 환불 완료 수량 집계 함수 호출
            int alreadyRefundedQty = refundItemRepository.sumQuantityByOrderItemId(item.getId());

            // 비즈니스 룰 검증: (기존 환불 수량 + 이번 요청 수량)이 원본 결제 주문 수량을 넘어서면 조작으로 판단
            if (alreadyRefundedQty + requestQty > item.getQuantity()) {
                throw new ServiceException(ErrorCode.EXCEEDED_REFUND_QUANTITY);
            }

            // 클라이언트 데이터 변조 방지: 결제 당시에 찍힌 스냅샷 단가(priceAtOrder)를 기준으로 환불 가치 적산
            totalRefundAmount += (item.getPriceAtOrder() * requestQty);

            // Rich Domain Model 패턴 지향: Product 엔티티 직접 획득 후 도메인 핵심 비즈니스 로직으로 재고 차감 복구 (더티 체킹)
            Product product = productRepository.findById(item.getProductId()).orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
            product.increaseStock(requestQty);

            // 영수증 마스터와 함께 저장될 상세 영수증 엔티티(RefundItem) 생성 빌드
            readyRefundItems.add(RefundItem.createRefundItem(item.getId(), requestQty));
        }

        // 3. 복합 정산 도메인 계산기(RefundCalculator) 가동 (Integer 기반 정수 등급 적립률 그대로 인계)
        long currentPointBalance = member.getPointBalance();
        RefundCalculator calculator = new RefundCalculator(
                totalRefundAmount,
                payment.getAmount(),
                payment.getUsedPoint(),
                payment.getPgRealAmount(),
                member.getMembershipGrade().getRewardRate(),
                currentPointBalance
        );

        // 4. [포인트 도메인 원장 통합 규격] 가동 - 사용분 적격 복구 및 선적립 포인트 몰수 처리
        if (calculator.getFinalPointRestoreAmount() > 0) {
            pointService.createHistory(member, payment, PointTransactionType.USE_RECOVERY, calculator.getFinalPointRestoreAmount());
        }

        // 포인트 부족으로 인한 마이너스 통장 개설 방지 (현재 잔액과 회수해야 할 포인트 중 최솟값만 안전 차감)
        long actualPointToDeduct = Math.min(currentPointBalance, calculator.getPointToRecoverFromEarn());
        if (actualPointToDeduct > 0) {
            pointService.createHistory(member, payment, PointTransactionType.EARN_FORFEIT, actualPointToDeduct);
        }

        // 5. 환불 트래킹 마스터 영수증 엔티티 최종 데이터베이스 영속화
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

        // 6. 트랜잭션 최하단 최후의 외부 바운더리: 포트원 외부 API를 향한 전자 실결제 취소 요청 발송
        if (calculator.getFinalPgCancelAmount() > 0) {
            try {
                portOneClient.cancelPayment(
                        payment.getPortonePaymentId(),
                        request.getReason(),
                        calculator.getFinalPgCancelAmount()
                );
                log.info("[포트원 PG 환불 성공] 거래키: {}, 취소액: {}원", payment.getPortonePaymentId(), calculator.getFinalPgCancelAmount());

            } catch (Exception e) {
                // 비즈니스 원장은 성공했으나 외부 통신망 에러로 카드 대금이 안 묶이도록 관리자 수동 조치 로그 파일 강제 적재
                log.error("[포트원 통신 실패] DB 데이터는 저장되었으나 실결제 취소 실패", e);
                log.error("수동 정산 대상 가동 요망 -> 거래키: {}, 금액: {}", payment.getPortonePaymentId(), calculator.getFinalPgCancelAmount());

                throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }
        }
    }
}
