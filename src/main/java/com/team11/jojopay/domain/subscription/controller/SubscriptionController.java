package com.team11.jojopay.domain.subscription.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionBillingResponse;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  @PostMapping
  public CommonApiResponse<SubscriptionResponse> startSubscription(
      @AuthenticationPrincipal Long memberId,
      @Valid @RequestBody SubscriptionStartRequest request
  ) {
    SubscriptionResponse response = subscriptionService.startSubscription(memberId, request);
    return CommonApiResponse.success(CREATED, "구독 시작 완료", response);
  }

  @GetMapping("/me")
  public CommonApiResponse<SubscriptionResponse> getMySubscription(
      @AuthenticationPrincipal Long memberId
  ) {
    SubscriptionResponse response = subscriptionService.getMySubscription(memberId);
    return CommonApiResponse.success(OK, "내 구독 조회 완료", response);
  }

  @PostMapping("/me/cancel")
  public CommonApiResponse<SubscriptionResponse> cancelSubscription(
      @AuthenticationPrincipal Long memberId
  ) {
    SubscriptionResponse response = subscriptionService.cancelSubscription(memberId);
    return CommonApiResponse.success(OK, "구독 해지 완료", response);
  }

  @GetMapping("/me/billings")
  public CommonApiResponse<List<SubscriptionBillingResponse>> getMySubscriptionBillings(
      @AuthenticationPrincipal Long memberId
  ) {
    List<SubscriptionBillingResponse> response = subscriptionService.getMySubscriptionBillings(memberId);
    return CommonApiResponse.success(OK, "내 구독 결제 내역 조회 완료", response);
  }
}
