package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.entity.SubscriptionBilling;
import com.team11.jojopay.domain.subscription.enums.SubscriptionPlan;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionBillingRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionTransactionService {

  private final MemberRepository memberRepository;
  private final BillingKeyRepository billingKeyRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionBillingRepository subscriptionBillingRepository;
  private final PointService pointService;

  /**
   * 첫 구독 결제 성공 후 필요한 DB 변경을 하나의 트랜잭션으로 처리
   *
   * 구독 생성, 첫 청구 이력 저장, 포인트 적립, 누적 결제금액 반영을 함께 커밋
   */
  @Transactional
  public SubscriptionResponse saveStartSubscriptionSuccess(
      Long memberId,
      Long billingKeyId,
      SubscriptionPlan plan,
      LocalDate nextBillingDate,
      String billingPeriod,
      String paymentId
  ) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

    BillingKey billingKey = billingKeyRepository.findById(billingKeyId)
        .orElseThrow(() -> new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND));

    Subscription subscription = Subscription.start(
        member,
        billingKey,
        plan,
        nextBillingDate
    );

    Subscription savedSubscription = subscriptionRepository.save(subscription);

    SubscriptionBilling subscriptionBilling = SubscriptionBilling.createSuccess(
        savedSubscription,
        1,
        billingPeriod,
        savedSubscription.getPrice(),
        paymentId
    );

    subscriptionBillingRepository.save(subscriptionBilling);

    Long earnPoint = calculateEarnPoint(member, subscription.getPrice());

    pointService.createHistory(
        member.getId(),
        null,
        PointTransactionType.EARN,
        earnPoint
    );

    member.increaseTotalPaymentAmount(savedSubscription.getPrice());

    return SubscriptionResponse.from(savedSubscription);
  }

  /**
   * 정기결제 성공 후 필요한 DB 변경을 하나의 트랜잭션으로 처리
   *
   * 구독 row에 비관적 락을 걸고 조회한 뒤,
   * 성공 청구 이력 저장, 포인트 저장, 누적 결제금액 반영,
   * 다음 결제일 갱신을 수행
   */
  @Transactional
  public void saveRenewSubscriptionSuccess(
      Long subscriptionId,
      int nextCycle,
      String billingPeriod,
      String paymentId
  ) {
    Subscription subscription = subscriptionRepository.findByIdWithPessimisticLock(subscriptionId)
        .orElseThrow(() -> new ServiceException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    Member member = subscription.getMember();

    SubscriptionBilling successBilling = SubscriptionBilling.createSuccess(
        subscription,
        nextCycle,
        billingPeriod,
        subscription.getPrice(),
        paymentId
    );

    subscriptionBillingRepository.save(successBilling);

    Long earnPoint = calculateEarnPoint(member, subscription.getPrice());

    pointService.createHistory(
        member.getId(),
        null,
        PointTransactionType.EARN,
        earnPoint
    );

    member.increaseTotalPaymentAmount(subscription.getPrice());

    subscription.updateNextBillingDate();
  }

  /**
   * 정기결제 실패 후 실패 청구 이력만 저장
   *
   * 과제 정책상 일사적인 카드 문제일 수 있으므로 구독 상태는 즉시 PAST_DUE로 변경하지 않음
   */
  @Transactional
  public void saveRenewSubscriptionFailure(
      Long subscriptionId,
      int nextCycle,
      String billingPeriod
  ) {
    Subscription subscription = subscriptionRepository.findByIdWithPessimisticLock(subscriptionId)
        .orElseThrow(() -> new ServiceException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    SubscriptionBilling failedBilling = SubscriptionBilling.createFailed(
        subscription,
        nextCycle,
        billingPeriod,
        subscription.getPrice()
    );

    subscriptionBillingRepository.save(failedBilling);
  }

  private Long calculateEarnPoint(Member member, Long price) {
    double earnRate = member.getMembershipGrade().getRewardRate() / 100.0;
    return (long) (price * earnRate);
  }
}
