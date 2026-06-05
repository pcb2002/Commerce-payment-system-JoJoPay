package com.team11.jojopay.domain.payment.repository;

import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  // ✅ 동시성 제어를 위해 비관적 쓰기 락(SELECT ... FOR UPDATE)을 걸고 조회하는 메서드 추가
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Payment p WHERE p.portonePaymentId = :portonePaymentId")
  Optional<Payment> findByPortonePaymentIdWithLock(@Param("portonePaymentId") String portonePaymentId);

  // ✅ 주문 정보를 기반으로 결제 내역 조회
  Optional<Payment> findByOrder(Order order);
}
