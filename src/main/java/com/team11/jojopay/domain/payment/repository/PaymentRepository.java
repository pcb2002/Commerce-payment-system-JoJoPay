package com.team11.jojopay.domain.payment.repository;

import com.team11.jojopay.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  // PortOne 결제 번호로 조회
  Optional<Payment> findByPortonePaymentId(String portonePaymentId);
}
