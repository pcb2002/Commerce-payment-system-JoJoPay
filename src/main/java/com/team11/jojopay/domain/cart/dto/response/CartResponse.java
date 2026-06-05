package com.team11.jojopay.domain.cart.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class CartResponse {

    private final Long cartId;

    private final List<CartItemResponse> cartItems;


    /**
     * 장바구니 전체 금액
     */
    private final Integer totalAmount;


    public CartResponse(
            Long cartId,
            List<CartItemResponse> cartItems,
            Integer totalAmount
    ) {

        this.cartId = cartId;
        this.cartItems = cartItems;
        this.totalAmount = totalAmount;
    }
}
