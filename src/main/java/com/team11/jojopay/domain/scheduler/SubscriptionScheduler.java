package com.team11.jojopay.domain.scheduler;

import com.team11.jojopay.domain.subscription.entity.Subscription;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionService subscriptionService;

  @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
  @Transactional
  public void processRecurringPayments() {
    LocalDate today = LocalDate.now();

    // 상태가 ACTIVE이고 다음 결제일이 오늘인 구독 조회
    List<Subscription> targets = subscriptionRepository
        .findAllByStatusAndNextBillingDate(SubscriptionStatus.ACTIVE, today);

    for (Subscription subscription : targets) {
      // 개별 구독 건에 대해 정기 결제 실행 로직 위임
      subscriptionService.renewSubscription(subscription);
    }
  }
}
