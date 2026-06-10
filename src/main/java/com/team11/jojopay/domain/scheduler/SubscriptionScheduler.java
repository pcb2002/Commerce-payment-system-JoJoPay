package com.team11.jojopay.domain.scheduler;

import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.repository.SubscriptionRepository;
import com.team11.jojopay.domain.subscription.service.SubscriptionService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionService subscriptionService;

  /**
   * 매일 새벽 자정에 당일 정기 구독 결제 대상자를 추출하여 자동 갱신을 진행합니다.
   * 크론 표현식: 초 분 시 일 월 요일
   */
  @Transactional
  @Scheduled(cron = "0 0 0 * * *")
  public void runSubscriptionRenewal() {
    LocalDate today = LocalDate.now();
    log.info("[정기 구독 배치 시작] 기준일자: {}", today);

    try {
      // 다중 서버 환경에서 중복 결제 요청이 PG사로 넘어가는 것을 막기 위해 비관적 락으로 대상자 조회
      List<Subscription> billingTargets = subscriptionRepository
          .findAllByStatusAndNextBillingDateWithLock(SubscriptionStatus.ACTIVE, today);

      log.info("[정기 구독 배치 조회 완료] 결제 대상 건수: {}건", billingTargets.size());

      // 대상자들을 순회하며 정기 결제 수행
      for (Subscription subscription : billingTargets) {
        try {
          // 개별 구독 건마다 내부에서 try-catch가 일어나므로, 특정 유저 한 명이 실패해도 나머지 유저들의 결제는 실패하지 않습니다.
          subscriptionService.renewSubscription(subscription);
        } catch (Exception e) {
          log.error("[정기 구독 배치 개별 실패] 구독 ID: {}, 사유: {}", subscription.getId(), e.getMessage());
        }
      }

    } catch (Exception e) {
      log.error("[정기 구독 배치 시스템 치명적 오류] 대상자 조회 단계 실패", e);
    }

    log.info("[정기 구독 배치 종료] 기준일자: {}", today);
  }
}
