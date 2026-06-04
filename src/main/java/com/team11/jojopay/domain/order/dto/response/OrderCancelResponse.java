package com.team11.jojopay.domain.order.dto.response;

import com.team11.jojopay.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 결제 대기 중인 주문의 취소 결과를 담는 응답 객체입니다.
 */
@Getter
@Builder
public class OrderCancelResponse {

    private String orderNumber;
    private String status; // CANCELLED
    private LocalDateTime cancelledAt;

    /**
     * 정적 팩토리 메서드: 취소 처리된 Order 엔티티를 받아 응답 DTO로 조립합니다.
     */
    public static OrderCancelResponse from(Order order) {
        return OrderCancelResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .cancelledAt(LocalDateTime.now()) // 취소된 현재 시간
                .build();
    }
}