package com.team11.jojopay.domain.subscription.dto.response;

import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import lombok.Getter;

@Getter
public class BillingKeyResponse {

  private final Long billingKeyId;

  private final String customerUid;

  private final String cardName;

  private final String cardNumber;

  private final BillingKeyStatus status;

  private BillingKeyResponse(
      Long billingKeyId,
      String customerUid,
      String cardName,
      String cardNumber,
      BillingKeyStatus status
  ) {
    this.billingKeyId = billingKeyId;
    this.customerUid = customerUid;
    this.cardName = cardName;
    this.cardNumber = cardNumber;
    this.status = status;
  }

  public static BillingKeyResponse from(BillingKey billingKey) {
    return new BillingKeyResponse(
        billingKey.getId(),
        billingKey.getCustomerUid(),
        billingKey.getCardName(),
        billingKey.getCardNumber(),
        billingKey.getStatus()
    );
  }
}
