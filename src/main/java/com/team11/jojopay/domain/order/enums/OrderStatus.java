package com.team11.jojopay.domain.order.enums;

public enum OrderStatus {
    PENDING_PAYMENT, // 결제 대기
    COMPLETED,       // 결제 완료
    CANCELLED,       // [결제 취소] 결제 전 취소 또는 결제 실패로 무산됨 (재고 복구 완료)
    PARTIAL_REFUND,  // [부분 환불] 일부 상품 품목만 환불 처리됨
    FULLY_REFUNDED   // [전체 환불] 결제 완료 후, 모든 상품이 최종 환불 처리됨
}
