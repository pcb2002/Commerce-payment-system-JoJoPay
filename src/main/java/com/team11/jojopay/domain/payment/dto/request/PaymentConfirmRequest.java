package com.team11.jojopay.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PaymentConfirmRequest {

  @NotBlank(message = "주문 번호는 필수입니다.")
  private String orderNumber;

  @NotBlank(message = "결제 고유 ID는 필수입니다.")
  private String portonePaymentId;
}
