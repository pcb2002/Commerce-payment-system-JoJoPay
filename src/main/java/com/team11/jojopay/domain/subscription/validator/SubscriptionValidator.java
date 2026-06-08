package com.team11.jojopay.domain.subscription.validator;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionValidator {

  public void validateBillingPaymentResult(
      PortOnePaymentResponse response,
      Long expectedAmount
  ) {
    if (response == null
        || !"PAID".equals(response.getStatus())
        || response.getAmount() == null
        || !expectedAmount.equals(response.getAmount().getTotal())) {
      throw new ServiceException(ErrorCode.VALIDATION_FAILED);
    }
  }

  public void validateNoActiveSubscription(Optional<Subscription> activeSubscription) {
    if (activeSubscription.isPresent()) {
      throw new ServiceException(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION);
    }
  }

  public void validateActiveBillingKey(BillingKey billingKey) {
    if (billingKey.getStatus() != BillingKeyStatus.ACTIVE) {
      throw new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND);
    }
  }
}
