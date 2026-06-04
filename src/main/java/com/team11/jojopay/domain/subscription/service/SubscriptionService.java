package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;

  private final MemberService memberService;

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

    LocalDateTime nextBillingDate = LocalDateTime.now().plusMonths(1);

    Subscription subscription = Subscription.start(
        member,
        request.getBillingKeyId(),
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
}
