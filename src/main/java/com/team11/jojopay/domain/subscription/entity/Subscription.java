package com.team11.jojopay.domain.subscription.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "billing_key_id", nullable = false)
  private BillingKey billingKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_name", nullable = false, length = 50)
  private SubscriptionPlan plan;

  @Column(nullable = false)
  private Long price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SubscriptionStatus status;

  @Column(name = "next_billing_date", nullable = false)
  private LocalDate nextBillingDate;

  private Subscription(
      Member member,
      BillingKey billingKey,
      SubscriptionPlan plan,
      LocalDate nextBillingDate) {
    this.member = member;
    this.billingKey = billingKey;
    this.plan = plan;
    this.price = plan.getPrice();
    this.status = SubscriptionStatus.ACTIVE;
    this.nextBillingDate = nextBillingDate;
  }

  /**
   * 구독 시작 시 ACTIVE 상태의 Subscription 객체를 생성
   */
  public static Subscription start(
      Member member,
      BillingKey billingKey,
      SubscriptionPlan plan,
      LocalDate nextBillingDate
  ) {
    return new Subscription(
        member,
        billingKey,
        plan,
        nextBillingDate
    );
  }

  /**
   * 구독을 해지 상태로 변경 이미 해지된 구독에 다시 호출되어도 같은 상태를 유지
   */
  public void cancel() {
    this.status = SubscriptionStatus.CANCELED;
  }

  /**
   * 다음 정기 결제일을 변경 스케줄러에서 정기 결제 성공 후 다음 결제일 갱신 시 사용
   */
  public void updateNextBillingDate() {
    // 정기 자동 결제 성공 시 차기 1달 이월
    this.nextBillingDate = this.nextBillingDate.plusMonths(1);
  }

  public void markAsPastDue() {
    this.status = SubscriptionStatus.PAST_DUE;
  }
}
