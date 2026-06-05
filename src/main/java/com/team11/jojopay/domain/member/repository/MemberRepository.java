package com.team11.jojopay.domain.member.repository;

import com.team11.jojopay.domain.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

  /**
   *
   * 이메일로 회원을 조회
   * 로그인 시 가입된 회원인지 확인할 때 사용
   */
  Optional<Member> findByEmail(String email);

  /**
   *
   * 이메일 중복 여부 확인
   * 회원가입 시 이미 사용 중인 이메일인지 검사할 때 사용
   */
  boolean existsByEmail(String email);

  /**
   * 구독 정기 결제 처리 시 회원의 포인트 및 누적 결제 금액을 안전하게 변경하기 위해
   * 회원 row에 비관적 락을 걸고 조회합니다.
   *
   * 같은 회원에게 동시에 결제/포인트 적립 요청이 들어올 경우
   * Lost Update를 방지하기 위해 사용합니다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Member m WHERE m.id = :memberId")
  Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);
}
