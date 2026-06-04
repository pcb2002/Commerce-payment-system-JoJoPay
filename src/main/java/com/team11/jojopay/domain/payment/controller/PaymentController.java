package com.team11.jojopay.domain.payment.controller;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * 클라이언트 결제 승인 요청 브라우저 결제창에서 결제가 완료된 후, 프론트엔드에서 서버로 승인 및 검증을 요청할 때 사용합니다.
   */
  @PostMapping("/confirm")
  public CommonApiResponse<PaymentResponse> confirmPayment(
      @Valid @RequestBody PaymentConfirmRequest request) {

    // 요청을 받아서 서비스에 넘긴다.
    PaymentResponse response = paymentService.confirmPayment(request);

    // 서비스가 준 결과를 공통 응답 포맷에 담아서 반환한다.
    return CommonApiResponse.success(OK, "결제 승인 완료", response);
  }

  /**
   * [API 2] 포트원 웹훅(Webhook) 수신 사용자가 결제 완료 후 브라우저를 종료하는 등의 상황을 대비하여, 포트원 서버가 우리 서버로 결제 결과를 비동기 통보해
   * 주는 엔드포인트입니다.
   */
  @PostMapping("/webhook")
  public CommonApiResponse<Void> handleWebhook(@RequestBody Map<String, String> payload) {
    // 포트원 웹훅 페이로드에서 payment_id 추출
    String portonePaymentId = payload.get("payment_id");

    // 결제 승인 로직 재사용 (서비스 내부의 멱등성 로직 덕분에 중복 처리되지 않음)
    paymentService.confirmPayment(new PaymentConfirmRequest(null, portonePaymentId));

    return CommonApiResponse.success(HttpStatus.OK, "웹훅 처리가 완료되었습니다.", null);
  }
}
