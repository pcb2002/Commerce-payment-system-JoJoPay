package com.team11.jojopay.domain.cartitem.repository;


import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;


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
}


