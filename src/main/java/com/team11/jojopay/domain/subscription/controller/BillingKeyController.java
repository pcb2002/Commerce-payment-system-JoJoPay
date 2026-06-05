package com.team11.jojopay.domain.subscription.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.subscription.dto.request.BillingKeyRegisterRequest;
import com.team11.jojopay.domain.subscription.dto.response.BillingKeyResponse;
import com.team11.jojopay.domain.subscription.service.BillingKeyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions/billing-keys")
public class BillingKeyController {

  private final BillingKeyService billingKeyService;

  /**
   * 빌링키 등록 API
   * 로그인한 회원의 결제수단 정보를 등록
   */
  @PostMapping
  public CommonApiResponse<BillingKeyResponse> registerBillingKey(
      @AuthenticationPrincipal Long memberId,
      @Valid @RequestBody BillingKeyRegisterRequest request
  ) {
    BillingKeyResponse response = billingKeyService.registerBillingKey(memberId, request);

    return CommonApiResponse.success(CREATED, "빌링키 등록 완료", response);
  }

  /**
   * 빌링키 목록 조회 API
   * 로그인한 회원의 활성 결제수단 목록을 조회
   */
  @GetMapping
  public CommonApiResponse<List<BillingKeyResponse>> getBillingKeys(
      @AuthenticationPrincipal Long memberId
  ) {
    List<BillingKeyResponse> responses = billingKeyService.getMyBillingKeys(memberId);

    return CommonApiResponse.success(OK, "빌링키 목록 조회 완료", responses);
  }

  /**
   * 빌링키 삭제 API
   * 로그인한 회원의 결제수단을 비활성화
   */
  @DeleteMapping("/{billingKeyId}")
  public CommonApiResponse<Void> deleteBillingKey(
      @AuthenticationPrincipal Long memberId,
      @PathVariable Long billingKeyId
  ) {
    billingKeyService.deleteBillingKey(memberId, billingKeyId);

    return CommonApiResponse.success(OK, "빌링키 삭제 완료", null);
  }
}
