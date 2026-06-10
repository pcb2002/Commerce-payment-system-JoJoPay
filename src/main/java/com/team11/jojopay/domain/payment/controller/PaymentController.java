package com.team11.jojopay.domain.payment.controller;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.service.PaymentService;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.team11.jojopay.common.exception.ErrorCode.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PortOneClient portOneClient;
  private final ObjectMapper objectMapper; // JSON 파싱용
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
  public CommonApiResponse<Void> handleWebhook(@RequestHeader("x-portone-signature") String signature, // 1. 서명 받기
                                               @RequestBody String rawBody) { // 2. Map 대신 String으로 원본 데이터 받기

    // 1. 서명 검증 실패 시 ErrorCode.UNAUTHORIZED 반환
    if (!portOneClient.verifyWebhookSignature(rawBody, signature)) {
      log.warn("🚨 웹훅 서명 검증 실패! 위변조된 요청입니다.");
      return CommonApiResponse.error(ErrorCode.UNAUTHORIZED);
    }

    try {
      // 2. JSON 파싱
      JsonNode root = objectMapper.readTree(rawBody);
      // 포트원 웹훅 페이로드 구조에 따라 path("data").path("payment_id") 부분을 확인하세요
      String portonePaymentId = root.path("data").path("payment_id").asText();

      log.info("✅ 웹훅 처리 시작: {}", portonePaymentId);

      // 3. 서비스 호출
      paymentService.confirmPayment(new PaymentConfirmRequest(null, portonePaymentId));

      // 성공 응답 (CommonApiResponse의 성공 규격에 맞춤)
      return CommonApiResponse.success(OK, "웹훅 처리가 완료되었습니다.", null);

    } catch (Exception e) {
      log.error("웹훅 처리 중 오류 발생: ", e);
      // 4. 예외 발생 시 ErrorCode.INTERNAL_SERVER_ERROR 반환
      return CommonApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
