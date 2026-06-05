package com.team11.jojopay.domain.member.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
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
        () -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND)
    );

    return MemberResponse.from(member);
  }

  @Transactional(readOnly = true)
  public MembershipResponse getMyMembership(Long memberId) {
    Member member = memberRepository.findById(memberId).orElseThrow(
        () -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND)
    );

    return MembershipResponse.from(member);
  }

  public Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND)); // 팀 내 에러코드 규격에 맞게 조정
  }
}
