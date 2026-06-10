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
}
