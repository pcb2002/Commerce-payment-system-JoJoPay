package com.team11.jojopay.domain.cartitem;

import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CartItemTest {

    private Cart createCart() {
        Member member = mock(Member.class);
        return new Cart(member);
    }

    private Product createProduct() {
        return mock(Product.class);
    }

    @Test
    @DisplayName("수량 증가 성공")
    void addQuantity_success() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        cartItem.addQuantity(2);
        assertEquals(5, cartItem.getQuantity());
    }

    @Test
    @DisplayName("수량 변경 성공")
    void changeQuantity_success() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        cartItem.changeQuantity(10);
        assertEquals(10, cartItem.getQuantity());
    }

    @Test
    @DisplayName("수량 변경 실패 - 0 ")
    void changeQuantity_fail() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        assertThrows(ServiceException.class, () -> cartItem.changeQuantity(0));
    }

    @Test
    @DisplayName("수량 변경 실패 - 음수")
    void changeQuantity_negative_fail() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        assertThrows(ServiceException.class, () -> cartItem.changeQuantity(-5));
    }

    @Test
    @DisplayName("수량 증가 실패 - 음수")
    void addQuantity_fail() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        assertThrows(ServiceException.class, () -> cartItem.addQuantity(-1));
    }

    @Test
    @DisplayName("수량 증가 실패 - 0")
    void addQuantity_zero_fail() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        assertThrows(ServiceException.class, () -> cartItem.addQuantity(0));
    }

    @Test
    @DisplayName("soft delete 성공")
    void softDelete_success() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        cartItem.softDelete();
        assertNotNull(cartItem.getDeletedAt());
    }

    @Test
    @DisplayName("상품 ID 조회")
    void getProductId_success() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(100L);

        CartItem cartItem = new CartItem(createCart(), product, 3);
        Long productId = cartItem.getProductId();

        assertEquals(100L, productId);
    }

    @Test
    @DisplayName("CartItem 생성 성공")
    void constructor_success() {
        Cart cart = createCart();
        Product product = createProduct();

        CartItem cartItem = new CartItem(cart, product, 5);

        assertEquals(cart, cartItem.getCart());
        assertEquals(product, cartItem.getProduct());
        assertEquals(5, cartItem.getQuantity());
    }

    @Test
    @DisplayName("soft delete 시 삭제시간 저장")
    void softDelete_setDeletedAt() {
        CartItem cartItem = new CartItem(createCart(), createProduct(), 3);
        cartItem.softDelete();

        assertNotNull(cartItem.getDeletedAt());
        assertTrue(cartItem.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("정적 팩토리 메서드 성공 검증: createCartItem 호출 시 유효성 검증을 거쳐 정상 인스턴스가 할당된다.")
    void createCartItem_Success() {
        // given
        Cart cart = createCart();
        Product product = createProduct();

        // when: 정적 팩토리 로직 라인 관통
        CartItem cartItem = CartItem.createCartItem(cart, product, 4);

        // then
        assertNotNull(cartItem);
        assertEquals(cart, cartItem.getCart());
        assertEquals(product, cartItem.getProduct());
        assertEquals(4, cartItem.getQuantity());
    }

    @Test
    @DisplayName("정적 팩토리 메서드 실패 검증: 수량 변동 자리에 부적절한 바인딩(0원/음수) 인입 시 예외가 스로우된다.")
    void createCartItem_Fail_InvalidQuantity() {
        // given
        Cart cart = createCart();
        Product product = createProduct();

        // when & then: changeQuantity(0) 내부 예외 방어선 도달 검증
        assertThrows(
                ServiceException.class,
                () -> CartItem.createCartItem(cart, product, 0)
        );
    }

    @Test
    @DisplayName("JPA NoArgsConstructor 커버: 리플렉션을 통해 무조건 무력화 상태인 PROTECTED 기본 생성자를 구동한다.")
    void protected_NoArgsConstructor_Coverage_Test() throws Exception {
        // given: 롬복의 AccessLevel.PROTECTED 기본 생성자 강제 획득
        java.lang.reflect.Constructor<CartItem> constructor = CartItem.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // when: 인스턴스화 가동
        CartItem instance = constructor.newInstance();

        // then: 널 레이어 사각지대 해소 확인
        assertNotNull(instance);
        assertNull(instance.getId());
        assertNull(instance.getCart());
    }
}
