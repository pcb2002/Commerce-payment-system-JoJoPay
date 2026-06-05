package com.team11.jojopay.domain.refund.repository;

import com.team11.jojopay.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Refund(환불 마스터 마이그레이션 데이터 영수증) 엔티티에 대한 영속성 데이터 액세스를 전담하는 레포지토리 인터페이스입니다.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

}
