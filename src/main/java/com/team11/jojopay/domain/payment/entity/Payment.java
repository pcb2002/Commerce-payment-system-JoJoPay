package com.team11.jojopay.domain.payment.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_order_id", columnNames = {"order_id"}),
        @UniqueConstraint(name = "uk_payment_portone_id", columnNames = {"portone_payment_id"})
    })
public class Payment extends BaseTimeEntity { // created_at, updated_at 상속

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // PK

  @Column(nullable = false, unique = true)
  private String portonePaymentId; // 포트원 고유 식별자
  private Long amount;
  private Long usedPoint;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;
  private LocalDateTime approvedAt;

  // 팩토리 메서드
  public static Payment createPayment(String portonePaymentId, Long amount, Long usedPoint) {
    Payment payment = new Payment();
    payment.portonePaymentId = portonePaymentId;
    payment.amount = amount;
    payment.usedPoint = usedPoint;
    payment.status = PaymentStatus.READY;
    return payment;
  }

  // 비즈니스 로직: 결제 완료 처리
  public void complete() {
    this.status = PaymentStatus.COMPLETED;
    this.approvedAt = LocalDateTime.now();
  }

  // 비즈니스 로직: 결제 실패 처리
  public void fail() {
    this.status = PaymentStatus.FAILED;
  }
}
