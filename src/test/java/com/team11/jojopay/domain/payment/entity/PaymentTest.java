package com.team11.jojopay.domain.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentTest {

  @Test
  @DisplayName("결제 완료 상태 전이 성공: complete 호출 시 READY 상태에서 COMPLETED로 변경된다.")
  void complete_Success() {
    // Mock 대신 CALLS_REAL_METHODS를 주어 엔티티 내부의 '진짜 메서드 로직'이 실행되도록 합니다.
    Payment payment = mock(Payment.class, CALLS_REAL_METHODS);

    // 리플렉션 기술로 엔티티 내부의 private 필드인 status를 READY로 강제 설정합니다.
    ReflectionTestUtils.setField(payment, "status", PaymentStatus.READY);

    // when: 결제 완료 메서드 트리거 (진짜 엔티티 내부 코드가 동작함)
    payment.complete();

    // then: 상태가 COMPLETED로 안전하게 변경되었는지 도메인 검증
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
  }

  @Test
  @DisplayName("결제 실패 상태 전이 성공: fail 호출 시 상태가 FAILED로 변경된다.")
  void fail_Success() {
    // given: 마찬가지로 실제 로직 객체 생성 후 READY 상태 주입
    Payment payment = mock(Payment.class, CALLS_REAL_METHODS);
    ReflectionTestUtils.setField(payment, "status", PaymentStatus.READY);

    // when: 결제 실패 메서드 트리거
    payment.fail();

    // then: 상태가 FAILED로 안전하게 변경되었는지 검증
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }
}