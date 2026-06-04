package com.team11.jojopay.domain.subscription.enums;

import lombok.Getter;

@Getter
public enum SubscriptionStatus {

  ACTIVE("활성"),
  CANCELED("해지"),
  PAST_DUE("미납");

  private final String description;

  SubscriptionStatus(String description) {
    this.description = description;
  }
}
