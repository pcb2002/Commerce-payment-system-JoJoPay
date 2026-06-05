package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.BillingKeyRepository;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final BillingKeyRepository billingKeyRepository;

  private final MemberService memberService;
  private final PortOneClient portOneClient;
  private final PointService pointService;

  @Transactional(readOnly = true)
  public SubscriptionResponse getMySubscription(Long memberId) {

    Subscription subscription = subscriptionRepository.findByMemberId(memberId)
        .orElseThrow(
            () -> new ServiceException(ErrorCode.SUBSCRIPTION_NOT_FOUND)
        );

    return SubscriptionResponse.from(subscription);
  }

  @Transactional
  public SubscriptionResponse startSubscription(
      Long memberId,
      SubscriptionStartRequest request
  ) {

    Member member = memberService.findMemberById(memberId);

    subscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE)
        .ifPresent(subscription -> {
          throw new ServiceException(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION);
        });

    LocalDate nextBillingDate = LocalDate.now().plusMonths(1);

    BillingKey billingKey = billingKeyRepository.findByIdAndMemberId(
        request.getBillingKeyId(),
        memberId
    ).orElseThrow(() -> new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND));

    if (billingKey.getStatus() != BillingKeyStatus.ACTIVE) {
      throw new ServiceException(ErrorCode.BILLING_KEY_NOT_FOUND);
    }

    portOneClient.scheduleBillingKeyPayment(
        billingKey.getCustomerUid(),
        "SUB_FIRST_" + memberId,
        request.getPlan().getPrice(),
        request.getPlan().getPlanName()
    );

    Subscription subscription = Subscription.start(
        member,
        billingKey,
        request.getPlan(),
        nextBillingDate
    );

    Subscription savedSubscription = subscriptionRepository.save(subscription);

    return SubscriptionResponse.from(savedSubscription);
  }

  @Transactional
  public SubscriptionResponse cancelSubscription(Long memberId) {

    Subscription subscription = subscriptionRepository.findByMemberIdAndStatus(
        memberId,
        SubscriptionStatus.ACTIVE
    ).orElseThrow(
        () -> new ServiceException(ErrorCode.NO_ACTIVE_SUBSCRIPTION)
    );

    subscription.cancel();

    return SubscriptionResponse.from(subscription);
  }

  @Transactional
  public void renewSubscription(Subscription subscription) {
    Member member = subscription.getMember();

    try {
      // 포트원 빌링키 결제 API 호출
      portOneClient.scheduleBillingKeyPayment(
          subscription.getBillingKey().getCustomerUid(),
          "SUB_" + subscription.getId(),
          subscription.getPrice(),
          subscription.getPlan().getPlanName());

      // 포인트 적립 (결제 직전 누적 금액 기준 등급 적용)
      // 현재 등급의 적립률을 가져와서 적립
      double earnRate = member.getMembershipGrade().getRewardRate() / 100.0;
      Long earnPoint = (long) (subscription.getPrice() * earnRate);
      // 메서드 추가 요청
      pointService.earnPoint(member, earnPoint, null);

      // 누적 금액 업데이트 및 등급 재계산
      member.increaseTotalPaymentAmount(subscription.getPrice());

      // 다음 결제일 갱신
      subscription.updateNextBillingDate();

    } catch (Exception e) {
      // 정기 결제 실패 시 청구서에 실패 기록 후 상태 유지
      subscription.markAsPastDue();
    }
  }
}
