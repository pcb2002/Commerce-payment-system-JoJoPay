package com.team11.jojopay.domain.order.reopsitory;

import com.team11.jojopay.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 보안 강화: 요청받은 OrderItem ID 목록이 실제 해당 회원의 주문인지 쿼리 단계에서 검증
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.id IN :orderItemIds AND o.memberId = :memberId")
    List<OrderItem> findAllByIdInAndOrderMemberId(
            @Param("orderItemIds") List<Long> orderItemIds,
            @Param("memberId") Long memberId
    );
}