package com.team11.jojopay.domain.refund.repository;

import com.team11.jojopay.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

}
