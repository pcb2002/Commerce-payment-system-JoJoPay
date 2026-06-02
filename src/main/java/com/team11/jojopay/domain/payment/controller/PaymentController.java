package com.team11.jojopay.domain.payment.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/confirm")
  public CommonApiResponse<PaymentResponse> confirmPayment(
      @Valid @RequestBody PaymentConfirmRequest request) {

    // 요청을 받아서 서비스에 넘긴다.
    PaymentResponse response = paymentService.confirmPayment(request);

    // 서비스가 준 결과를 공통 응답 포맷에 담아서 반환한다.
    return CommonApiResponse.success(HttpStatus.OK, "결제 승인 완료", response);
  }
}
