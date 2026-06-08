package com.team11.jojopay.domain.cartitem.entity;


import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "cart_items", uniqueConstraints = {@UniqueConstraint(name = "uk_cart_product",
columnNames = {"cart_id", "product_id"})}
/**
 * UNIQUE(cart_id, product_id) → 같은 장바구니에 동일 상품 중복으로 저장되는거 방지
 * 기존 수량에 더해지는 정책 사용
*/
)
public class CartItem extends BaseTimeEntity {

    // 장바구니 상품 pk
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 장바구니 참조
     * 하나의 장바구니에 여러 상품이 담길 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * 상품 참조
     * 하나의 상품은 여러 장바구니에 담길 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 상품 수량
    @Column(nullable = false)
    private Integer quantity;

    // 장바구니 상품 삭제 처리
    @Column
    private LocalDateTime deletedAt;

    public static CartItem createCartItem(Cart cart, Product product, Integer quantity) {
        CartItem cartItem = new CartItem();
        cartItem.cart = cart;
        cartItem.product = product;
        cartItem.changeQuantity(quantity); // 생성 시에도 검증 로직 통과하게 유도
        return cartItem;
    }

    /**
     * OrderService 에서 상품 ID를 바로 꺼내 쓰기 위한 편의 메서드
     */
    public Long getProductId() {
        return this.product.getId();
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    // 수량 증가 - 동일 상품 다시 담을 시 사용
    public void addQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ServiceException(ErrorCode.INVALID_STOCK_VALUE);
        }
        this.quantity += quantity;
    }

    // 수량 변경 - 장바구니 수정 기능에서 사용
    public void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ServiceException(ErrorCode.INVALID_STOCK_VALUE);
        }
        this.quantity = quantity;
    }

    /**
     * Soft Delete 처리
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public CartItem(
            Cart cart,
            Product product,
            Integer quantity
    ) {

        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }
}
