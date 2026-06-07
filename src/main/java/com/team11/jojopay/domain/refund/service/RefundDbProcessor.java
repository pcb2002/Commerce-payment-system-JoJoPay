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
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));
        refund.updateStatus(status);
    }
}