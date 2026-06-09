package com.team11.jojopay.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team11.jojopay.domain.cart.dto.response.CartItemResponse;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.product.entity.Product;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartResponseDtoTest {

    // ==========================================
    // 1. CartResponse 롬복 Getter 및 생성자 라인 커버
    // ==========================================
    @Test
    @DisplayName("CartResponse 모델 생성 및 필드 데이터 정합성 검증")
    void cartResponse_Fields_And_Getter_Test() {
        // given
        Long expectedCartId = 10L;
        Integer expectedTotalAmount = 50000;
        CartItemResponse mockItem = mock(CartItemResponse.class);
        List<CartItemResponse> expectedItems = List.of(mockItem);

        // when: 생성자 라인 관통
        CartResponse cartResponse = new CartResponse(expectedCartId, expectedItems, expectedTotalAmount);

        // then: 롬복 Getter 사각지대 제거
        assertThat(cartResponse.getCartId()).isEqualTo(expectedCartId);
        assertThat(cartResponse.getCartItems()).hasSize(1);
        assertThat(cartResponse.getTotalAmount()).isEqualTo(expectedTotalAmount);
    }

    // ==========================================
    // 2. CartItemResponse 정적 팩토리 메서드(.from) 연산 라인 커버
    // ==========================================
    @Test
    @DisplayName("CartItemResponse 팩토리 검증: 엔티티 장부 규격과 수량 곱셈 연산 로직이 오차 없이 매핑된다.")
    void cartItemResponse_From_Entity_Calculations_Test() {
        // given: 연산 부품 조립을 위한 가짜 엔티티 Mocking
        CartItem mockCartItem = mock(CartItem.class);
        Product mockProduct = mock(Product.class);

        // 상품 원장 명세 세팅
        when(mockProduct.getId()).thenReturn(55L);
        when(mockProduct.getName()).thenReturn("아이패드 프로");
        when(mockProduct.getPrice()).thenReturn(1200000L); // 단가: 1,200,000원

        // 장바구니 품목 명세 세팅
        when(mockCartItem.getId()).thenReturn(777L);
        when(mockCartItem.getProduct()).thenReturn(mockProduct);
        when(mockCartItem.getQuantity()).thenReturn(3); // 수량: 3개

        // when: DTO 내부의 정적 팩토리 .from() 메서드 라인 다이렉트 타격
        CartItemResponse response = CartItemResponse.from(mockCartItem);

        // then: 연산 결과 명세 및 Getter 수집 검증
        assertThat(response.getCartItemId()).isEqualTo(777L);
        assertThat(response.getProductId()).isEqualTo(55L);
        assertThat(response.getProductName()).isEqualTo("아이패드 프로");
        assertThat(response.getProductPrice()).isEqualTo(1200000L);
        assertThat(response.getQuantity()).isEqualTo(3);
        
        // 단가 1,200,000원 * 3개 = 3,600,000원 공식이 내부에서 정확히 계산되었는지 체크하여 커버리지를 흡수합니다.
        assertThat(response.getTotalPrice()).isEqualTo(3600000L);
    }
}
