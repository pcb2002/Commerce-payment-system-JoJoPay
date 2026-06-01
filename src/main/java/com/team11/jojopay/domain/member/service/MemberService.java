package com.team11.jojopay.domain.member.service;

import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public MemberResponse getMyInfo(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(
        () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
    );

    return MemberResponse.from(member);
  }

  @Transactional(readOnly = true)
  public MembershipResponse getMyMembership(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(
        () -> new IllegalArgumentException("존재하지 않는 회원입니다.")
    );

    return MembershipResponse.from(member);
  }
}
