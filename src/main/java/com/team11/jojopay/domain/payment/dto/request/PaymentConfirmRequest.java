package com.team11.jojopay.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 파싱을 위해 필요
@AllArgsConstructor // 직접 객체를 생성할 때 필요
public class PaymentConfirmRequest {

  @NotBlank(message = "주문 번호는 필수입니다.")
  private String orderNumber;

  @NotBlank(message = "결제 고유 ID는 필수입니다.")
  private String portonePaymentId;
}
