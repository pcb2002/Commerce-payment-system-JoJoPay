package com.team11.jojopay.domain.order.dto.response;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {
    private String orderNumber;
    private Long totalAmount;
    private Long usedPoint;
    private Long pgRealAmount;
    private String portonePaymentId;

    public static OrderResponse of(Order order, Payment payment, Long pgRealAmount) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .usedPoint(order.getUsedPoint())
                .pgRealAmount(pgRealAmount)
                .portonePaymentId(payment.getPortonePaymentId())
                .build();
    }
}