package com.team11.jojopay.domain.subscription.repository;

import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  Optional<Subscription> findByMemberId(Long memberId);

  /**
   * 회원 ID와 구독 상태로 구독 정보를 조회
   * 구독 중복 시작 방지 및 해지 대상 구독 조회
   */
  Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

  // 스케줄러 연동을 위한 배치 쿼리 메서드 추가
  List<Subscription> findAllByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate nextBillingDate);
}
