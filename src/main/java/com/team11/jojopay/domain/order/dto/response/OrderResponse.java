package com.team11.jojopay.domain.order.dto.response;

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
}