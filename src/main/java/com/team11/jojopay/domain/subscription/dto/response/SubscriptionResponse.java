package com.team11.jojopay.domain.subscription.dto.response;

import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SubscriptionResponse {

  private final Long subscriptionId;
  private final String planName;
  private final Long price;
  private final SubscriptionStatus status;
  private final LocalDateTime nextBillingDate;

  private SubscriptionResponse(
      Long subscriptionId,
      String planName,
      Long price,
      SubscriptionStatus status,
      LocalDateTime nextBillingDate
  ) {
    this.subscriptionId = subscriptionId;
    this.planName = planName;
    this.price = price;
    this.status = status;
    this.nextBillingDate = nextBillingDate;
  }

  public static SubscriptionResponse from(Subscription subscription) {
    return new SubscriptionResponse(
        subscription.getId(),
        subscription.getPlan().getPlanName(),
        subscription.getPrice(),
        subscription.getStatus(),
        subscription.getNextBillingDate()
    );
  }
}
