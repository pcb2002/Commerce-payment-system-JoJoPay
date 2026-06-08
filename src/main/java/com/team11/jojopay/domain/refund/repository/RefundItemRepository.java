package com.team11.jojopay.domain.refund.repository;

import com.team11.jojopay.domain.refund.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * RefundItem(환불 영수증 상세 아이템 항목) 엔티티에 대한 데이터베이스 직결 액세스를 관리하는 레포지토리 인터페이스입니다.
 */
@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    /**
     * 특정 주문 상품 식별자(OrderItem ID)를 기준으로 과거부터 현재까지 누적으로 처리 완료된 환불 수량의 총합을 연산합니다.
     * 이 메서드의 연산 결과값은 신규 부분 환불 진행 시 잔여 환불 가능 수량을 초과했는지 판단하는 상한선 밸리데이션 검증에 활용됩니다.
     *
     * @param id 검증하고자 하는 원본 주문 상품 상세 내역의 고유 식별자 PK (orderItemId)
     * @return 해당 주문 상품에서 이미 환불 처리가 완료되어 소멸된 수량의 총 합산값
     */
    @Query("select coalesce(sum(ri.quantity), 0) from RefundItem ri where ri.orderItemId = :orderItemId")
    int sumQuantityByOrderItemId(Long id);
}
