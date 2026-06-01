package com.team11.jojopay.domain.member.controller;

import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/me")
  public ResponseEntity<MemberResponse> getMyInfo(@RequestParam Long memberId) {
    MemberResponse response = memberService.getMyInfo(memberId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me/membership")
  public ResponseEntity<MembershipResponse> getMyMembership(@RequestParam Long memberId) {
    MembershipResponse response = memberService.getMyMembership(memberId);
    return ResponseEntity.ok(response);
  }
}
