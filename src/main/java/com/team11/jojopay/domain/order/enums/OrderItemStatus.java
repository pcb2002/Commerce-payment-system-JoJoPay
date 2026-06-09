package com.team11.jojopay.domain.order.enums;

public enum OrderItemStatus {
    COMPLETED, // 결제 완료 (기본값)
    REFUNDED   // 환불 완료 (이 상품 품목은 완전 취소됨)
}
