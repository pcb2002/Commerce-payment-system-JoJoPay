package com.team11.jojopay.domain.payment.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.point.entity.PointHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

  // ✅ Order와의 1:1 연관관계 매핑
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

  @Column(nullable = false, unique = true)
  private String portonePaymentId; // 포트원 고유 식별자
  private Long amount;
  private Long usedPoint;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @OneToMany(mappedBy = "payment")
  private List<PointHistory> pointHistories = new ArrayList<>();

  private LocalDateTime approvedAt;

  /**
   * 정적 팩토리 메서드
   * 생성 시점에 Order 객체를 직접 받습니다.
   */
  public static Payment createPayment(Order order, String portonePaymentId, Long amount, Long usedPoint) {
    Payment payment = new Payment();
    payment.order = order; // 연관관계 매핑
    payment.portonePaymentId = portonePaymentId;
    payment.amount = amount;
    payment.usedPoint = usedPoint;
    payment.status = PaymentStatus.READY;
    return payment;
  }

  /**
   * [Rich Domain Model] 결제 대기 상태를 취소 상태로 변경합니다.
   * 주로 결제 도중 사용자가 이탈하거나 검증 실패 시 호출됩니다.
   */
  public void cancel() {
        this.status = PaymentStatus.CANCELED;
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
