package com.team11.jojopay.domain.subscription.enums;

import lombok.Getter;

@Getter
public enum SubscriptionPlan {
  BASIC("베이직", 9900L),
  STANDARD("스탠다드", 19900L),
  PREMIUM("프리미엄", 29900L);

  private final String planName;

  private final Long price;

  SubscriptionPlan(String planName, Long price) {
    this.planName = planName;
    this.price = price;
  }
}
