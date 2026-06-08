package com.team11.jojopay.domain.order.dto.response;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 단건 주문 상세 조회 응답 객체입니다.
 * 주문 당시의 상품 스냅샷 정보와 실제 결제/포인트 사용 내역을 포함합니다.
 */
@Getter
@Builder
public class OrderDetailResponse {

    private String orderNumber;
    private String status;
    private Long totalAmount;
    private LocalDateTime createdAt;

    // 주문에 포함된 상품들의 스냅샷 리스트
    private List<OrderItemSnapshotResponse> orderItems;

    // 결제 및 포인트 사용 요약 정보
    private PaymentSummaryResponse payment;

    public static OrderDetailResponse of(Order order, Payment payment) {
        return OrderDetailResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                // 하위 컬렉션 변환
                .orderItems(order.getOrderItems().stream()
                        .map(OrderItemSnapshotResponse::from)
                        .collect(Collectors.toList()))
                // 결제 정보 변환
                .payment(PaymentSummaryResponse.from(payment))
                .build();
    }

    /**
     * 주문 상세 내역에 포함되는 주문 상품 스냅샷 정보
     */
    @Getter
    @Builder
    public static class OrderItemSnapshotResponse {
        private Long productId;
        private String productName;      // 주문 당시의 상품명 (스냅샷)
        private Long priceAtOrder;    // 주문 당시의 판매가 (스냅샷)
        private Integer quantity;
        private String status;

        public static OrderItemSnapshotResponse from(OrderItem item) {
            return OrderItemSnapshotResponse.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .priceAtOrder(item.getPriceAtOrder())
                    .quantity(item.getQuantity())
                    .status(item.getStatus().name())
                    .build();
        }
    }

    /**
     * 주문 상세 내역에 포함되는 결제 요약 정보
     */
    @Getter
    @Builder
    public static class PaymentSummaryResponse {
        private String portonePaymentId; // PG사 연동 결제 고유 ID
        private String paymentStatus;    // 결제 진행 상태 (READY, PAID, FAILED 등)
        private Long usedPoint;       // 사용한 포인트
        private Long pgRealAmount;    // 실제 카드로 결제된 금액

        public static PaymentSummaryResponse from(Payment payment) {
            return PaymentSummaryResponse.builder()
                    .portonePaymentId(payment.getPortonePaymentId())
                    .paymentStatus(payment.getStatus().name())
                    .usedPoint(payment.getUsedPoint())
                    .pgRealAmount(payment.getAmount())
                    .build();
        }
    }
}