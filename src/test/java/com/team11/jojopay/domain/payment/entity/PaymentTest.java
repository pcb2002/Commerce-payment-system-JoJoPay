package com.team11.jojopay.domain.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  @DisplayName("결제 완료 상태 전이 성공: complete 호출 시 READY 상태에서 COMPLETED로 변경된다.")
  void complete_Success() {
    // given: READY 상태의 Payment 엔티티 준비 (생성자 또는 빌더 구조 반영)
    Order mockOrder = mock(Order.class);
    Payment payment = Payment.builder()
        .amount(50000L)
        .usedPoint(5000L)
        .pgRealAmount(45000L)
        .status(PaymentStatus.READY)
        .order(mockOrder)
        .build();

    // when: 결제 완료 메서드 트리거
    payment.complete();

    // then: 상태가 COMPLETED로 안전하게 변경되었는지 도메인 검증
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    // 내부에 approvedAt(승인시간) 필드가 있다면 함께 검증 가능
    // assertThat(payment.getApprovedAt()).isNotNull();
  }

  @Test
  @DisplayName("결제 실패 상태 전이 성공: fail 호출 시 상태가 FAILED로 변경된다.")
  void fail_Success() {
    // given: READY 상태의 Payment 엔티티 준비
    Payment payment = Payment.builder()
        .amount(30000L)
        .usedPoint(0L)
        .pgRealAmount(30000L)
        .status(PaymentStatus.READY)
        .build();

    // when: 결제 실패 메서드 트리거
    payment.fail();

    // then: 상태가 FAILED로 안전하게 변경되었는지 검증
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }
}