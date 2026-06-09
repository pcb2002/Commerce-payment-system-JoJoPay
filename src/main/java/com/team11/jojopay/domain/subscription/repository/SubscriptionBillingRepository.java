package com.team11.jojopay.domain.subscription.repository;

import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, Long> {

  List<SubscriptionBilling> findAllBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

  long countBySubscriptionIdAndStatus(Long subscriptionId, SubscriptionBillingStatus status);
}
