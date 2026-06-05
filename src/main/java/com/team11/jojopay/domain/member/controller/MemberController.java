package com.team11.jojopay.domain.member.controller;

import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/me")
  public CommonApiResponse<MemberResponse> getMyInfo(
      @AuthenticationPrincipal Long memberId
  ) {
    MemberResponse response = memberService.getMyInfo(memberId);

    return CommonApiResponse.success(OK, "회원 정보 조회 성공", response);
  }

  @GetMapping("/me/membership")
  public CommonApiResponse<MembershipResponse> getMyMembership(
      @AuthenticationPrincipal Long memberId
  ) {
    MembershipResponse response = memberService.getMyMembership(memberId);

    return CommonApiResponse.success(OK, "멤버십 정보 조회 성공", response);
  }
}
