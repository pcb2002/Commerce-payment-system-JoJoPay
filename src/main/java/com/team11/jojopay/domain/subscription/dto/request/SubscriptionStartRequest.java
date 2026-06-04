package com.team11.jojopay.domain.subscription.dto.request;

import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SubscriptionStartRequest {

  @NotNull(message = "결제수단 ID는 필수입니다.")
  private String billingKeyId;

  @NotNull(message = "구독 플랜은 필수입니다.")
  private SubscriptionPlan plan;
}
