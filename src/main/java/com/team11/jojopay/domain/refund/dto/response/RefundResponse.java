package com.team11.jojopay.domain.refund.dto.response;

import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.entity.RefundItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RefundResponse {

    private final Long refundId;
    private final String orderNumber;
    private final String reason;
    private final Long totalRefundAmount;
    private final Long pointRefundAmount;
    private final Long pgRefundAmount;
    private final String status;
    private final LocalDateTime createdAt;
    private final List<RefundItemDetail> refundItems;

    private RefundResponse(Long refundId, String orderNumber, String reason,
                           Long totalRefundAmount, Long pointRefundAmount, Long pgRefundAmount,
                           String status, LocalDateTime createdAt, List<RefundItemDetail> refundItems) {
        this.refundId = refundId;
        this.orderNumber = orderNumber;
        this.reason = reason;
        this.totalRefundAmount = totalRefundAmount;
        this.pointRefundAmount = pointRefundAmount;
        this.pgRefundAmount = pgRefundAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.refundItems = refundItems;
    }

    public static RefundResponse from(Refund refund) {
        List<RefundItemDetail> items = refund.getRefundItems().stream()
                .map(RefundItemDetail::from)
                .toList();

        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getOrder().getOrderNumber(),
                refund.getReason(),
                refund.getTotalRefundAmount(),
                refund.getPointRefundAmount(),
                refund.getPgRefundAmount(),
                refund.getStatus().name(),
                refund.getCreatedAt(),
                items
        );
    }

    @Getter
    public static class RefundItemDetail {
        private final Long orderItemId;
        private final Integer quantity;

        private RefundItemDetail(Long orderItemId, Integer quantity) {
            this.orderItemId = orderItemId;
            this.quantity = quantity;
        }

        public static RefundItemDetail from(RefundItem item) {
            return new RefundItemDetail(item.getOrderItemId(), item.getQuantity());
        }
    }
}