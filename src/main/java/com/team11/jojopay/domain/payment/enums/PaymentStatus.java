package com.team11.jojopay.domain.payment.enums;

public enum PaymentStatus {
  READY,      // 결제 대기
  COMPLETED,  // 결제 완료
  FAILED,     // 결제 실패
  CANCELED    // 결제 취소
}
