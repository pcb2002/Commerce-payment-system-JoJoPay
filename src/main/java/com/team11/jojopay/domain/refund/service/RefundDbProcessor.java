package com.team11.jojopay.domain.refund.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.enums.OrderItemStatus;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.entity.RefundItem;
import com.team11.jojopay.domain.refund.enums.RefundStatus;
import com.team11.jojopay.domain.refund.repository.RefundItemRepository;
import com.team11.jojopay.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RefundDbProcessor {

    private final OrderValidator orderValidator;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;
    private final MemberService memberService;

    /**
     * [트랜잭션 1] 모든 도메인 로직을 단일 트랜잭션으로 처리 후 READY 상태로 커밋합니다.
     */
    @Transactional
    public Refund saveRefundAndRollbackStock(Long memberId, RefundRequest request) {
        Order order = orderValidator.validateAndGetOrder(request.getOrderNumber(), memberId);
        Payment payment = orderValidator.validateAndGetPayment(order);
        Member member = memberService.findMemberById(order.getMemberId());

        List<Long> requestItemIds = request.getItems().stream().map(RefundRequest.RefundItemRequest::getOrderItemId).toList();
        List<OrderItem> refundItems = orderValidator.validateAndGetOrderItems(requestItemIds, memberId);

        Map<Long, Integer> requestQuantityMap = request.getItems().stream().collect(Collectors.toMap(RefundRequest.RefundItemRequest::getOrderItemId, RefundRequest.RefundItemRequest::getQuantity));

        long totalRefundAmount = 0;
        List<RefundItem> readyRefundItems = new ArrayList<>();

        for (OrderItem item : refundItems) {
            // 이미 완전히 환불 처리 끝난 상품 품목이라면 요청을 원천 차단
            if (item.getStatus() == OrderItemStatus.REFUNDED) {
                throw new ServiceException(ErrorCode.ALREADY_REFUNDED_ITEM); // 예: 이미 환불된 상품입니다.
            }
            int requestQty = requestQuantityMap.get(item.getId());
            int alreadyRefundedQty = refundItemRepository.sumQuantityByOrderItemId(item.getId());

            if (alreadyRefundedQty + requestQty > item.getQuantity()) {
                throw new ServiceException(ErrorCode.EXCEEDED_REFUND_QUANTITY);
            }

            totalRefundAmount += (item.getPriceAtOrder() * requestQty);

            Product product = orderValidator.validateAndGetProductWithLock(item.getProductId());
            product.increaseStock(requestQty);

            readyRefundItems.add(RefundItem.createRefundItem(item.getId(), requestQty));
        }

        long currentPointBalance = member.getPointBalance();
        RefundCalculator calculator = new RefundCalculator(totalRefundAmount, payment.getAmount(), payment.getUsedPoint(), payment.getPgRealAmount(), member.getMembershipGrade().getRewardRate(), currentPointBalance);

        if (calculator.getFinalPointRestoreAmount() > 0) {
            pointService.createHistory(member.getId(), payment, PointTransactionType.USE_RECOVERY, calculator.getFinalPointRestoreAmount());
        }

        long actualPointToDeduct = Math.min(currentPointBalance, calculator.getPointToRecoverFromEarn());
        if (actualPointToDeduct > 0) {
            pointService.createHistory(member.getId(), payment, PointTransactionType.EARN_FORFEIT, actualPointToDeduct);
        }

        Refund refund = Refund.createRefund(payment, request.getReason(), calculator.getTotalRefundAmount(), calculator.getFinalPointRestoreAmount(), calculator.getFinalPgCancelAmount());

        for (RefundItem ri : readyRefundItems) {
            refund.addRefundItem(ri);
        }

        return refundRepository.save(refund);
    }

    /**
     * [트랜잭션 2] 외부 PG 연동 결과를 바탕으로 최종 상태(COMPLETED 또는 FAILED)를 확정합니다.
     */
    @Transactional
    public void updateRefundStatus(Long refundId, RefundStatus status) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));

        refund.updateStatus(status);

        // 🎯 [추가] 환불이 최종 성공(COMPLETED) 상태로 결정되었을 때만 주문 상태를 업데이트합니다.
        if (status == RefundStatus.COMPLETED) {
            Order order = refund.getPayment().getOrder();

            // 이번 환불 영수증(Refund)에 포함된 환불 품목(RefundItem)들을 순회
            refund.getRefundItems().forEach(refundItem -> {
                // 1. 환불 품목 ID를 통해 원본 주문 상품(OrderItem)을 조회
                OrderItem orderItem = orderValidator.validateAndGetOrderItems(
                        List.of(refundItem.getOrderItemId()),
                        order.getMemberId()
                ).get(0);

                // 2. [수량 조건 검증] 지금까지 누적 환불된 수량을 DB에서 긁어옴
                // (트랜잭션 1에서 이미 현재 환불 건이 insert 되었으므로, sumQuantity에 이번 수량도 포함되어 있음)
                int totalRefundedQty = refundItemRepository.sumQuantityByOrderItemId(orderItem.getId());

                // 3. 만약 누적 환불 완료된 수량이 처음 구매했던 수량과 같거나 크다면, 이 품목은 완전히 취소(전멸)된 것임!
                if (totalRefundedQty >= orderItem.getQuantity()) {
                    orderItem.refund(); // OrderItemStatus를 REFUNDED로 변경!
                }
            });

            // 4. 모든 하위 상품들의 상태 스캔이 끝났으므로, 상위 Order 원장 상태를 자동 갱신 (PARTIAL_REFUND 또는 FULLY_REFUNDED)
            order.updateStatusByItems();
        }
    }
}