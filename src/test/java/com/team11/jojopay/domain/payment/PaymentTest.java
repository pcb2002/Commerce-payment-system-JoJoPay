package com.team11.jojopay.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  @DisplayName("일반 결제 생성 성공: 초기 상태는 READY이며 복합 포인트 차감액이 안전하게 자동 계산된다.")
  void createPayment_Success() {
    // given
    Order mockOrder = mock(Order.class);

    // when: 기존 builder 대신 진짜 도메인 생성 팩토리 메서드 타격
    Payment payment = Payment.createPayment(mockOrder, 42L, "imp_123456", 150000L, 20000L);

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
    assertThat(payment.getAmount()).isEqualTo(150000L);
    assertThat(payment.getUsedPoint()).isEqualTo(20000L);
    assertThat(payment.getPgRealAmount()).isEqualTo(130000L); // 150000 - 20000 자동 연산 검증
    assertThat(payment.getOrder()).isEqualTo(mockOrder);
  }

  @Test
  @DisplayName("구독 결제 생성 성공: 부모 주문 객체는 null로 격리되며 정기 결제용 상수 정보가 알맞게 세팅된다.")
  void createSubscriptionPayment_Success() {
    // when: 구독 전용 정적 팩토리 메서드 검증 라인 밟기
    Payment payment = Payment.createSubscriptionPayment(7L, "imp_sub_999", 19900L, "TOSS_PAYMENTS");

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
    assertThat(payment.getSubscriptionId()).isEqualTo(7L);
    assertThat(payment.getOrder()).isNull();
    assertThat(payment.getPgProvider()).isEqualTo("TOSS_PAYMENTS");
    assertThat(payment.getPaymentMethod()).isEqualTo("CARD");
  }

  @Test
  @DisplayName("결제 완료 상태 전이 성공: complete 호출 시 READY 상태에서 COMPLETED로 변경되고 승인 시각이 기록된다.")
  void complete_Success() {
    // given
    Payment payment = Payment.createPayment(mock(Order.class), 42L, "imp_123", 50000L, 0L);

    // when
    payment.complete();

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThat(payment.getApprovedAt()).isNotNull();
  }

  @Test
  @DisplayName("결제 실패 상태 전이 성공: fail 호출 시 상태가 FAILED로 변경된다.")
  void fail_Success() {
    // given
    Payment payment = Payment.createPayment(mock(Order.class), 42L, "imp_123", 50000L, 0L);

    // when
    payment.fail();

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  @DisplayName("결제 취소 상태 전이 성공: cancel 호출 시 상태가 CANCELED로 전이된다.")
  void cancel_Success() {
    // given:  누락되었던 cancel 메서드 검증 분기 추가
    Payment payment = Payment.createPayment(mock(Order.class), 42L, "imp_123", 50000L, 0L);

    // when
    payment.cancel();

    // then
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
  }
}