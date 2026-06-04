package com.team11.jojopay.domain.order.dto.response;

import com.team11.jojopay.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 내 주문 내역 목록 조회 시, 개별 주문 항목을 담는 응답 객체입니다.
 */
@Getter
@Builder
public class OrderListItemResponse {

    private String orderNumber;
    private String status; // 주문/결제 상태 (예: PENDING_PAYMENT, COMPLETED 등)
    private Long totalAmount; // 총 결제 금액
    private LocalDateTime createdAt; // 주문 일시

    public static OrderListItemResponse from(Order order) {
        return OrderListItemResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }
}