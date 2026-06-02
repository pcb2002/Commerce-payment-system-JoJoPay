package com.team11.jojopay.domain.order.reopsitory;

import com.team11.jojopay.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 주문(Order) 도메인의 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 1. 특정 주문 단건 조회 (소유권 검증 포함)
     * 결제 진행, 주문 취소 등에서 사용됩니다.
     * 다른 회원이 악의적으로 주문 번호를 입력해 접근하는 것을 막기 위해 반드시 memberId를 함께 조건으로 사용합니다.
     *
     * @param orderNumber 노출용 고유 주문번호
     * @param memberId 요청을 보낸 인증된 회원 ID
     * @return 소유권이 일치하는 주문 객체 (Optional)
     */
    Optional<Order> findByOrderNumberAndMemberId(String orderNumber, Long memberId);

    /**
     * 2. 내 주문 내역 목록 조회 (페이징 지원)
     * '내 주문 내역 조회' API에서 사용됩니다.
     * 최신순 정렬은 Pageable 객체 생성 시 Sort.by(Direction.DESC, "createdAt")를 넘겨서 처리하거나
     * 메서드명에 OrderByCreatedAtDesc를 붙여 구현할 수 있습니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이징 및 정렬 정보
     * @return 회원의 주문 목록 (Page 객체)
     */
    Page<Order> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 3. 주문 상세 조회 (N+1 문제 방지용 Fetch Join)
     * '주문 상세 조회' API에서 사용됩니다.
     * 주문을 조회할 때 연관된 주문 상품(OrderItems)들을 지연 로딩(Lazy)으로 인해
     * 건건이 추가 쿼리로 가져오는 N+1 문제를 방지하기 위해 @EntityGraph를 사용하여 한 번의 쿼리로 조인하여 가져옵니다.
     *
     * @param orderNumber 노출용 고유 주문번호
     * @param memberId 회원 ID
     * @return 연관된 주문 상품이 페치 조인된 주문 객체 (Optional)
     */
    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Order> findWithOrderItemsByOrderNumberAndMemberId(String orderNumber, Long memberId);
}