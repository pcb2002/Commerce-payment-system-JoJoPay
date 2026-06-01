package com.team11.jojopay.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentCreateRequest {

  @NotNull(message = "주문 번호는 필수입니다.")
  private Long orderId;

  @NotNull(message = "결제 금액은 필수입니다.")
  @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다.")
  private Integer amount;
}
