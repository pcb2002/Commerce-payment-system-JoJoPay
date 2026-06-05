package com.team11.jojopay.domain.subscription.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.subscription.enums.SubscriptionBillingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "subscription_billings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionBilling extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id", nullable = false)
  private Subscription subscription;

  // 구독 결제 회차, 예: 첫 결제 1회차, 다음 정기 결제 2회차
  @Column(name = "billing_cycle", nullable = false)
  private Integer billingCycle;

  // 해당 결제가 적용되는 구독 이용 기간
  @Column(name = "billing_period", nullable = false, length = 50)
  private String billingPeriod;

  @Column(nullable = false)
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "billing_status", nullable = false, length = 20)
  private SubscriptionBillingStatus billingStatus;

  // PortOne에 전달한 결제 식별자, 결제 추적 및 조회에 사용
  @Column(name = "portone_tier_payment_id", length = 100)
  private String portoneTierPaymentId;

  private SubscriptionBilling(
      Subscription subscription,
      Integer billingCycle,
      String billingPeriod,
      Long amount,
      SubscriptionBillingStatus billingStatus,
      String portoneTierPaymentId
  ) {
    this.subscription = subscription;
    this.billingCycle = billingCycle;
    this.billingPeriod = billingPeriod;
    this.amount = amount;
    this.billingStatus = billingStatus;
    this.portoneTierPaymentId = portoneTierPaymentId;
  }

  /**
   * 구독 결제 성공 이력을 생성
   * 첫 결제 또는 정기 결제 성공 시 사용
   *
   * @param portoneTierPaymentId PortOne에 요청한 결제 식볋자
   */
  public static SubscriptionBilling createSuccess(
      Subscription subscription,
      Integer billingCycle,
      String billingPeriod,
      Long amount,
      String portoneTierPaymentId
  ) {
    return new SubscriptionBilling(
        subscription,
        billingCycle,
        billingPeriod,
        amount,
        SubscriptionBillingStatus.SUCCESS,
        portoneTierPaymentId
    );
  }

  /**
   * 구독 결제 실패 이력을 생성
   * 정기 결제 실패 시 사용
   * 실패 응답에서는 결제 식별자가 없을 수 있어 null로 저장
   */
  public static SubscriptionBilling createFailed(
      Subscription subscription,
      Integer billingCycle,
      String billingPeriod,
      Long amount
  ) {
    return new SubscriptionBilling(
        subscription,
        billingCycle,
        billingPeriod,
        amount,
        SubscriptionBillingStatus.FAILED,
        null
    );
  }
}
