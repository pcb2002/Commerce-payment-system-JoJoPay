package com.team11.jojopay.domain.payment.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
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
        .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

    // 이미 완료된 결제인지 체크
    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
    }

    // 외부 API 교차 검증
    Long realPaidAmount = 120000L; // 실제 구현 시 PortOne API 호출 결과

    // 금액 위변조 확인
    if (!payment.getAmount().equals(realPaidAmount)) {
      payment.fail(); // 상태를 실패로 변경
      throw new ServiceException(ErrorCode.VALIDATION_FAILED);
    }

    // 모든 검증 통과 시 결제 완료 처리
    payment.complete();

    // 장바구니 비우기, 포인트 적립 로직
    return PaymentResponse.from(payment);
  }
}
