package com.team11.jojopay.domain.cartitem.repository;


import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 장바구니 상품 전체 조회 - 삭제되지 않은 상품만
    List<CartItem> findByCartIdAndDeletedAtIsNull(Long cartId);


    // 동일 상품 존재 여부 조회 - 삭제되지 않은 데이터만
    Optional<CartItem> findByCartIdAndProductIdAndDeletedAtIsNull(Long cartId, Long productId);


    // 특정 장바구니 상품 조회
    Optional<CartItem> findByIdAndDeletedAtIsNull(Long cartItemId);


    // 장바구니 전체 상품 조회
    List<CartItem> findByCartAndDeletedAtIsNull(Cart cart);

    /**
     * [주문서 미리보기용] 특정 회원의 장바구니 전체 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT ci FROM CartItem ci " +
            "JOIN ci.cart c " +
            "WHERE c.member.id = :memberId AND ci.deletedAt IS NULL")
    List<CartItem> findAllByMemberId(@Param("memberId") Long memberId);

    /**
     * [주문 생성 / 선택 미리보기용] 특정 회원의 장바구니 항목 중, 사용자가 선택한 ID 리스트만 필터링하여 조회
     */
    @Query("SELECT ci FROM CartItem ci " +
            "JOIN ci.cart c " +
            "WHERE ci.id IN :cartItemIds AND c.member.id = :memberId AND ci.deletedAt IS NULL")
    List<CartItem> findAllByIdInAndMemberId(
            @Param("cartItemIds") List<Long> cartItemIds,
            @Param("memberId") Long memberId
    );
}