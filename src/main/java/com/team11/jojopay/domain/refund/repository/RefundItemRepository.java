package com.team11.jojopay.domain.refund.repository;

import com.team11.jojopay.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    int sumQuantityByOrderItemId(Long id);
}
