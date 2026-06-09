package com.team11.jojopay.domain.cart.service;


import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cart.repository.CartRepository;
import com.team11.jojopay.domain.cart.service.CartService;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberRepository memberRepository;


    /** 전체 흐름
     * 상품 담기 성공
     *
     * 검증 내용
     * 1. 회원 존재
     * 2. 장바구니 존재
     * 3. 상품 존재
     * 4. 판매 가능 상태
     * 5. 재고 충분
     * 6. 동일 상품 없음
     *
     * 결과
     * CartItem 저장
     */

    /**
     * AddCartItemRequest 생성 헬퍼 메서드
     *
     * 팀 규칙상 Request DTO에 생성자와 Setter를 만들지 않으므로
     * 테스트에서 ReflectionTestUtils를 사용해 값을 주입하여 진행.
     */
    private AddCartItemRequest createAddCartItemRequest(
            Long productId,
            Integer quantity
    ) {

        AddCartItemRequest request =
                new AddCartItemRequest();

        ReflectionTestUtils.setField(
                request,
                "productId",
                productId
        );

        ReflectionTestUtils.setField(
                request,
                "quantity",
                quantity
        );

        return request;
    }

    @Test
    @DisplayName("상품 담기 성공")
    void addCartItem_success() {

        // given

        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = mock(Cart.class);

        Product product = mock(Product.class);

        AddCartItemRequest request =
                createAddCartItemRequest(
                        1L,
                        2
                );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.ON_SALE);

        when(product.getStock())
                .thenReturn(10);

        when(product.getId())
                .thenReturn(1L);

        when(cart.getId())
                .thenReturn(1L);

        when(cartItemRepository
                .findByCartIdAndProductIdAndDeletedAtIsNull(
                        1L,
                        1L
                ))
                .thenReturn(Optional.empty());

        // when

        cartService.addCartItem(
                memberId,
                request
        );

        // then

        verify(cartItemRepository)
                .save(any(CartItem.class));
    }

    /**
     * 회원이 존재하지 않으면
     * MEMBER_NOT_FOUND 예외 발생
     */
    @Test
    @DisplayName("회원 없음")
    void memberNotFound() {

        // given

        AddCartItemRequest request =
                createAddCartItemRequest(
                        1L,
                        1
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.empty());

        // when & then

        assertThrows(
                ServiceException.class,
                () -> cartService.addCartItem(
                        1L,
                        request
                )
        );
    }

    /**
     * 상품이 존재하지 않으면
     * PRODUCT_NOT_FOUND 예외 발생
     */
    @Test
    @DisplayName("상품 없음")
    void productNotFound() {

        // given

        Member member = mock(Member.class);

        Cart cart = mock(Cart.class);

        AddCartItemRequest request =
                createAddCartItemRequest(
                        1L,
                        1
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        // when & then

        assertThrows(
                ServiceException.class,
                () -> cartService.addCartItem(
                        1L,
                        request
                )
        );
    }

    /**
     * 재고 초과 검증
     */
    @Test
    @DisplayName("재고 초과")
    void insufficientStock() {

        // given

        Member member = mock(Member.class);

        Cart cart = mock(Cart.class);

        Product product = mock(Product.class);

        AddCartItemRequest request =
                createAddCartItemRequest(
                        1L,
                        10
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.ON_SALE);

        when(product.getStock())
                .thenReturn(3);

        // when & then

        assertThrows(
                ServiceException.class,
                () -> cartService.addCartItem(
                        1L,
                        request
                )
        );

    }

    /**
     * 동일 상품 재담기
     *
     * 기존 수량 3개
     * 추가 수량 2개
     *
     * 결과
     * addQuantity(2) 호출
     */
    @Test
    @DisplayName("동일 상품 수량 합산")
    void mergeQuantity() {

        // given

        Long memberId = 1L;

        Member member = mock(Member.class);

        Cart cart = mock(Cart.class);

        Product product = mock(Product.class);

        CartItem cartItem = mock(CartItem.class);

        AddCartItemRequest request =
                createAddCartItemRequest(
                        1L,
                        2
                );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.ON_SALE);

        when(product.getStock())
                .thenReturn(10);

        when(product.getId())
                .thenReturn(1L);

        when(cart.getId())
                .thenReturn(1L);

        when(cartItem.getQuantity())
                .thenReturn(3);

        when(cartItemRepository
                .findByCartIdAndProductIdAndDeletedAtIsNull(
                        1L,
                        1L
                ))
                .thenReturn(Optional.of(cartItem));

        // when

        cartService.addCartItem(
                memberId,
                request
        );

        // then

        verify(cartItem)
                .addQuantity(2);
    }
}
