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

  @Column(name = "pg_real_amount", nullable = false)
  private Long pgRealAmount;

  @Column(nullable = false)
  private Long usedPoint; // 결제 시 사용한 복합 포인트

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status;

  @OneToMany(mappedBy = "payment")
  private List<PointHistory> pointHistories = new ArrayList<>();

  // 정산 및 사후 관리를 위한 PG사 메타 정보 컬럼 (ERD 기준 추가)
  @Column(name = "pg_provider", length = 50)
  private String pgProvider; // 예: KGINICIS, TOSS_PAYMENTS

  @Column(name = "payment_method", length = 30)
  private String paymentMethod; // 예: CARD, POINT

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
   * [구독 결제용] 정적 팩토리 메서드
   */
  public static Payment createSubscriptionPayment(Long subscriptionId, String portonePaymentId, Long amount, String pgProvider) {
    Payment payment = new Payment();
    payment.order = null; // 구독 결제이므로 주문은 null
    payment.subscriptionId = subscriptionId;
    payment.portonePaymentId = portonePaymentId;
    payment.amount = amount;
    payment.usedPoint = 0L; // 구독 결제는 포인트 복합결제가 없으므로 0
    // 구독 결제는 포인트를 쓰지 않으므로 pg_real_amount가 요금 전체(amount)가 됩니다.
    payment.pgRealAmount = amount;
    payment.pgProvider = pgProvider; // "TOSS_PAYMENTS" 상수 주입
    payment.paymentMethod = "CARD";
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
