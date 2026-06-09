package com.team11.jojopay.cartitem;


import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartItemTest {

    /**
     * Cart 생성용 메서드
     *
     */
    private Cart createCart() {

        Member member = mock(Member.class);

        return new Cart(member);
    }

    /**
     * Product Mock 생성
     */
    private Product createProduct() {

        return mock(Product.class);
    }

    /**
     * 동일 상품을 다시 장바구니에 담을 경우 수량이 증가하는지 검증
     */
    @Test
    @DisplayName("수량 증가 성공")
    void addQuantity_success() {

        // given
        CartItem cartItem =
                new CartItem(
                        createCart(),
                        createProduct(),
                        3
                );

        // when
        cartItem.addQuantity(2);

        // then
        assertEquals(
                5,
                cartItem.getQuantity()
        );
    }

    /**
     * 장바구니 수량 변경 기능 검증
     */
    @Test
    @DisplayName("수량 변경 성공")
    void changeQuantity_success() {

        // given
        CartItem cartItem =
                new CartItem(
                        createCart(),
                        createProduct(),
                        3
                );

        // when
        cartItem.changeQuantity(10);

        // then
        assertEquals(
                10,
                cartItem.getQuantity()
        );
    }

    /**
     * 0 입력 시 예외 발생 검증
     */
    @Test
    @DisplayName("수량 변경 실패 - 0 이하")
    void changeQuantity_fail() {

        // given
        CartItem cartItem =
                new CartItem(
                        createCart(),
                        createProduct(),
                        3
                );

        // when & then
        assertThrows(
                ServiceException.class,
                () -> cartItem.changeQuantity(0)
        );
    }

    /**
     * addQuantity 음수 허용 X
     */
    @Test
    @DisplayName("수량 증가 실패 - 음수")
    void addQuantity_fail() {

        // given
        CartItem cartItem =
                new CartItem(
                        createCart(),
                        createProduct(),
                        3
                );

        // when & then
        assertThrows(
                ServiceException.class,
                () -> cartItem.addQuantity(-1)
        );
    }

    /**
     * soft delete 수행 시 deletedAt 값이 설정되는지 검증
     */
    @Test
    @DisplayName("soft delete 성공")
    void softDelete_success() {

        // given
        CartItem cartItem =
                new CartItem(
                        createCart(),
                        createProduct(),
                        3
                );

        // when
        cartItem.softDelete();

        // then
        assertNotNull(
                cartItem.getDeletedAt()
        );
    }

}
