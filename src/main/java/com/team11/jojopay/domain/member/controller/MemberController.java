package com.team11.jojopay.domain.member.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

  private final MemberService memberService;
  private final JwtProvider jwtProvider;

  @GetMapping("/me")
  public ResponseEntity<CommonApiResponse<MemberResponse>> getMyInfo(
      @RequestHeader("Authorization") String authorizationHeader
  ) {
    Long memberId = extractMemberId(authorizationHeader);

    MemberResponse response = memberService.getMyInfo(memberId);

    return ResponseEntity.ok(
        CommonApiResponse.success(HttpStatus.OK, "회원 정보 조회 성공", response)
    );
  }

  @GetMapping("/me/membership")
  public ResponseEntity<CommonApiResponse<MembershipResponse>> getMyMembership(
      @RequestHeader("Authorization") String authorizationHeader
  ) {
    Long memberId = extractMemberId(authorizationHeader);

    MembershipResponse response = memberService.getMyMembership(memberId);

    return ResponseEntity.ok(
        CommonApiResponse.success(HttpStatus.OK, "멤버십 정보 조회 성공", response)
    );
  }

  /**
   * Authorization 헤더의 JWT 토큰에서 회원 ID를 추출
   */
  private Long extractMemberId(String authorizationHeader) {
    String token = authorizationHeader.replace("Bearer ", "");
    return jwtProvider.getMemberIdFromToken(token);
  }
}
