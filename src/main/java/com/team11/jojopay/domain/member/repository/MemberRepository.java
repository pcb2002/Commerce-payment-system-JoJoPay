package com.team11.jojopay.domain.member.repository;

import com.team11.jojopay.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
