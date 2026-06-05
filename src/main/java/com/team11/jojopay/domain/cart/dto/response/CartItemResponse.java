package com.team11.jojopay.domain.cart.dto.response;


import com.team11.jojopay.domain.cartitem.entity.CartItem;
import lombok.Getter;

@Getter
public class CartItemResponse {


    private final Long cartItemId;


    private final Long productId;


    private final String productName;


    private final Long productPrice;


    private final Integer quantity;


    /**
     * 상품 총 금액
     *
     * 가격 * 수량
     */
    private final Long totalPrice;


    public CartItemResponse(
            Long cartItemId,
            Long productId,
            String productName,
            Long productPrice,
            Integer quantity,
            Long totalPrice
    ) {

        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }



    public static CartItemResponse from(CartItem cartItem) {

        Long totalPrice =
                cartItem.getProduct().getPrice()
                        * cartItem.getQuantity();

        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity(),
                totalPrice
        );
    }
}

