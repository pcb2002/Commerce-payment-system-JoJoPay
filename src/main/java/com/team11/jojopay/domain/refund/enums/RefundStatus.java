package com.team11.jojopay.domain.refund.enums;

public enum RefundStatus {
    READY,      // 내부 DB 검증 및 재고/포인트 처리 완료 (PG 취소 전)
    COMPLETED,  // 외부 PG사 취소 최종 성공
    FAILED      // 외부 PG사 취소 통신 실패 (수동 정산 대상)
}
