package com.team11.jojopay.domain.payment.service;

import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.h2.api.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private final PaymentRepository paymentRepository;

  @Transactional
  public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
    // 기존 결제 정보 조회
    Payment payment = paymentRepository.findByPortonePaymentId(request.getPortonePaymentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

    // 이미 완료된 결제인지 체크
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      throw new BusinessException(ErrorCode.ALREADY_PROCESSED);
    }

    // 외부 API 교차 검증
    Long realPaidAmount = mockPortOneApiCall(request.getPortonePaymentId());

    // 금액 위변조 확인
    if (!payment.getAmount().equals(realPaidAmount)) {
      payment.fail(); // 상태를 실패로 변경
      throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    // 모든 검증 통과 시 결제 완료 처리
    payment.complete();

    // 장바구니 비우기, 포인트 적립 로직
    return PaymentResponse.from(payment);
  }

  // 포트원 서버와 통신한다고 가정하는 메서드
  private Long mockPortOneApiCall(String portonePaymentId) {
    // 실제로는 RestTemplate이나 WebClient를 사용하여 API 호출
    // 여기서는 하드코딩된 값을 반환하여 시뮬레이션
    return 10000L; // 예시 금액
}
