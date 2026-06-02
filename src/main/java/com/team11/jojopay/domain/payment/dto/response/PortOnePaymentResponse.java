package com.team11.jojopay.domain.payment.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포트원 V2 API로부터 받는 결제 상세 응답 DTO입니다.
 */
@Getter
@NoArgsConstructor
public class PortOnePaymentResponse {
  private String id;      // PortOne 결제 고유 ID
  private String status;  // 결제 상태 (PAID, READY 등)
  private Amount amount;  // 결제 금액 정보

  @Getter
  @NoArgsConstructor
  public static class Amount {
    private Long total; // 실제 결제된 총 금액
  }

}
