package com.team11.jojopay.domain.subscription.service;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final PortOneClient portOneClient;
  private final PointService pointService;

  @Transactional
  public void renewSubscription(Subscription subscription) {
    Member member = subscription.getMember();

    try {
      // 포트원 빌링키 결제 API 호출
      portOneClient.payWithBillingKey(subscription.getBillingKey(), subscription.getAmount());

      // 포인트 적립 (결제 직전 누적 금액 기준 등급 적용)
      // 현재 등급의 적립률을 가져와서 적립
      double earnRate = member.getMembershipGrade().getEarnRate();
      Long earnPoint = (long) (subscription.getAmount() * earnRate);
      pointService.earnPoint(member, earnPoint, null);

      // 누적 금액 업데이트 및 등급 재계산
      member.increaseTotalPaymentAmount(subscription.getAmount());

      // 다음 결제일 갱신
      subscription.updateNextBillingDate();

    } catch (Exception e) {
      // 정기 결제 실패 시 청구서에 실패 기록 후 상태 유지
      recordSubscriptionFail(subscription, e.getMessage());
    }
  }
}
