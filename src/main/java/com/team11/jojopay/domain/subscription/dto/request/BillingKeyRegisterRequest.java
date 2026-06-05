package com.team11.jojopay.domain.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BillingKeyRegisterRequest {

  @NotBlank(message = "customerUid는 필수입니다.")
  @Size(max = 255, message = "customerUid는 최대 255자까지 가능합니다.")
  private String customerUid;

  @NotBlank(message = "카드사는 필수입니다.")
  @Size(max = 50, message = "카드사는 최대 50자까지 가능합니다.")
  private String cardName;

  @NotBlank(message = "카드번호는 필수입니다.")
  @Size(max = 20, message = "카드번호는 최대 20자까지 가능합니다.")
  private String cardNumber;
}
