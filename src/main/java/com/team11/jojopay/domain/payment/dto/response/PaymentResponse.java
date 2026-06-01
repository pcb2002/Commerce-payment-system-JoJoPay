package com.team11.jojopay.domain.payment.dto.response;

import com.team11.jojopay.domain.payment.entity.Payment;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PaymentResponse {

  private final Long paymentId;
  private final String status;
  private final LocalDateTime approvedAt;

  private PaymentResponse(Long paymentId, String status, LocalDateTime approvedAt) {
    this.paymentId = paymentId;
    this.status = status;
    this.approvedAt = approvedAt;
  }

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getStatus().name(), // Enum을 String으로 변환
        payment.getApprovedAt()
    );
  }
}
