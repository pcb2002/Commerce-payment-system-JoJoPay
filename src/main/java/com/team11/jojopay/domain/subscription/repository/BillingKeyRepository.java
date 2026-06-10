package com.team11.jojopay.domain.subscription.repository;

import com.team11.jojopay.domain.subscription.entity.BillingKey;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원별 결제수단 조회, customerUid 중복 확인 등 사용
 */
public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

  // 동일한 customerUid가 이미 등록되어 있는지 확인
  boolean existsByCustomerUid(String customerUid);

  // 특정 회원이 소유한 빌링키 조회
  // 다른 회원의 결제수단 접근을 방지하기 위해 memberId 조건을 함께 사용
  Optional<BillingKey> findByIdAndMemberId(Long id, Long memberId);

  // 특정 회원의 특정 상태 결제수단 목록을 조회
  // 예: ACTIVE 상태의 결제수단만 조회
  List<BillingKey> findAllByMemberIdAndStatus(Long memberId, BillingKeyStatus status);
}
