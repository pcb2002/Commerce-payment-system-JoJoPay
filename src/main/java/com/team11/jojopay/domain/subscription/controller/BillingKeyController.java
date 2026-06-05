package com.team11.jojopay.domain.subscription.controller;

import static org.springframework.http.HttpStatus.CREATED;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.subscription.dto.request.BillingKeyRegisterRequest;
import com.team11.jojopay.domain.subscription.dto.response.BillingKeyResponse;
import com.team11.jojopay.domain.subscription.service.BillingKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BillingKeyController {

  private final BillingKeyService billingKeyService;

  /**
   * 빌링키 등록 API
   * 로그인한 회원의 결제수단 정보를 등록
   */
  @PostMapping("/api/v1/subscriptions/billing-keys")
  public CommonApiResponse<BillingKeyResponse> registerBillingKey(
      @AuthenticationPrincipal Long memberId,
      @Valid @RequestBody BillingKeyRegisterRequest request
  ) {
    BillingKeyResponse response = billingKeyService.registerBillingKey(memberId, request);

    return CommonApiResponse.success(CREATED, "빌링키 등록 완료", response);
  }
}
