package com.team11.jojopay.domain.cart.repository;


import com.team11.jojopay.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 회원 id로 장바구니 조회
     * 회원당 1개의 장바구니 보유
     */
    Optional<Cart> findByMemberId(Long memberId);
}
