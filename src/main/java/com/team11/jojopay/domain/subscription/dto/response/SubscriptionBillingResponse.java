package com.team11.jojopay.domain.subscription.dto.response;

import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class SubscriptionBillingResponse {

  private final Long subscriptionBillingId;
  private final Integer billingCycle;
  private final String billingPeriod;
  private final Long amount;
  private final SubscriptionBillingStatus billingStatus;
  private final String portoneTierPaymentId;
  private final LocalDateTime createdAt;

  private SubscriptionBillingResponse(
      Long subscriptionBillingId,
      Integer billingCycle,
      String billingPeriod,
      Long amount,
      SubscriptionBillingStatus billingStatus,
      String portoneTierPaymentId,
      LocalDateTime createdAt
  ) {
    this.subscriptionBillingId = subscriptionBillingId;
    this.billingCycle = billingCycle;
    this.billingPeriod = billingPeriod;
    this.amount = amount;
    this.billingStatus = billingStatus;
    this.portoneTierPaymentId = portoneTierPaymentId;
    this.createdAt = createdAt;
  }

  public static SubscriptionBillingResponse from(SubscriptionBilling subscriptionBilling) {
    return new SubscriptionBillingResponse(
        subscriptionBilling.getId(),
        subscriptionBilling.getBillingCycle(),
        subscriptionBilling.getBillingPeriod(),
        subscriptionBilling.getAmount(),
        subscriptionBilling.getBillingStatus(),
        subscriptionBilling.getPortoneTierPaymentId(),
        subscriptionBilling.getCreatedAt()
    );
  }
}
