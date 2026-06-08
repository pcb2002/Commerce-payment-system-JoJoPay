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


  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from Member m where m.id = :memberId")
  Optional<Member> findByIdWithLock(@Param("memberId") Long memberId);
}
