package com.team11.jojopay.domain.payment.entity;

import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long orderId;

  @Column(nullable = false)
  private Integer amount;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  private LocalDateTime createdAt;
  private LocalDateTime approvedAt;

  // 팩토리 메서드
  public static Payment create(Long orderId, Integer amount) {
    Payment payment = new Payment();
    payment.orderId = orderId;
    payment.amount = amount;
    payment.status = PaymentStatus.READY;
    payment.createdAt = LocalDateTime.now();
    return payment;
  }

  // 비즈니스 로직
  public void complete() {
    this.status = PaymentStatus.COMPLETED;
    this.approvedAt = LocalDateTime.now();
  }

  public void fail() {
    this.status = PaymentStatus.FAILED;
  }
}
