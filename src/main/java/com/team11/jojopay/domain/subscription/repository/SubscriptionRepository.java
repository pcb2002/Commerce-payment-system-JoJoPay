package com.team11.jojopay.domain.subscription.repository;

import com.team11.jojopay.domain.subscription.entity.Subscription;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  Optional<Subscription> findByMemberId(Long memberId);

  /**
   * 회원 ID와 구독 상태로 구독 정보를 조회 구독 중복 시작 방지 및 해지 대상 구독 조회
   */
  Optional<Subscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

  // 스케줄러 연동을 위한 배치 쿼리 메서드 추가
  //다중 서버 환경에서 중복 결제를 차단하기 위한 비관적 배타 락(Select for update) 적용
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT s 
      FROM Subscription s
      JOIN FETCH s.billingKey
      JOIN FETCH s.member       
      WHERE s.status = :status 
        AND s.nextBillingDate = :nextBillingDate""")
  List<Subscription> findAllByStatusAndNextBillingDateWithLock(
      @Param("status") SubscriptionStatus status,
      @Param("nextBillingDate") LocalDate nextBillingDate);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Subscription s WHERE s.id = :subscriptionId")
  Optional<Subscription> findByIdWithPessimisticLock(@Param("subscriptionId") Long subscriptionId);
}
