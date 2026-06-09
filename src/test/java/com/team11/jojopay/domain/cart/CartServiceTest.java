package com.team11.jojopay.domain.cart;


import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.dto.request.UpdateCartItemQuantityRequest;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    /**
     * 단종 상품 예외
     */
    @Test
    @DisplayName("단종 상품은 장바구니에 담을 수 없습니다.")
    void addCartItem_discontinuedProduct() {

        Member member = mock(Member.class);
        Cart cart = mock(Cart.class);
        Product product = mock(Product.class);

        AddCartItemRequest request =
                createAddCartItemRequest(1L, 1);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.DISCONTINUED);

        assertThrows(
                ServiceException.class,
                () -> cartService.addCartItem(1L, request)
        );
    }

    /**
     * 합산 후 재고 초과
     */
    @Test
    @DisplayName("기존 수량 + 요청 수량이 재고를 초과하면 실패")
    void mergeQuantity_insufficientStock() {

        Member member = mock(Member.class);
        Cart cart = mock(Cart.class);
        Product product = mock(Product.class);
        CartItem cartItem = mock(CartItem.class);

        AddCartItemRequest request =
                createAddCartItemRequest(1L, 5);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.ON_SALE);

        when(product.getStock())
                .thenReturn(5);

        when(product.getId())
                .thenReturn(1L);

        when(cart.getId())
                .thenReturn(1L);

        when(cartItem.getQuantity())
                .thenReturn(3);

        when(cartItemRepository
                .findByCartIdAndProductIdAndDeletedAtIsNull(
                        1L,
                        1L))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                ServiceException.class,
                () -> cartService.addCartItem(1L, request)
        );
    }

    /**
     * 장바구니 자동 생성
     */
    @Test
    @DisplayName("장바구니가 없으면 새로 생성합니다.")
    void createCartWhenNotExists() {

        Member member = mock(Member.class);
        Product product = mock(Product.class);

        Cart savedCart = mock(Cart.class);

        AddCartItemRequest request =
                createAddCartItemRequest(1L, 1);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenReturn(savedCart);

        when(savedCart.getId())
                .thenReturn(1L);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(product.getStatus())
                .thenReturn(ProductStatus.ON_SALE);

        when(product.getStock())
                .thenReturn(10);

        when(product.getId())
                .thenReturn(1L);

        when(cartItemRepository
                .findByCartIdAndProductIdAndDeletedAtIsNull(
                        1L,
                        1L))
                .thenReturn(Optional.empty());

        cartService.addCartItem(1L, request);

        verify(cartRepository)
                .save(any(Cart.class));
    }

    /**
     * 장바구니 수량 변경
     */
    @Test
    @DisplayName("장바구니 수량 변경 성공")
    void updateQuantity_success() {

        Cart cart = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);
        Product product = mock(Product.class);

        UpdateCartItemQuantityRequest request =
                new UpdateCartItemQuantityRequest();

        ReflectionTestUtils.setField(
                request,
                "quantity",
                5
        );

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(cartItem));

        when(cart.getId()).thenReturn(1L);

        when(cartItem.getCart()).thenReturn(cart);

        when(cartItem.getProduct()).thenReturn(product);

        when(product.getStock()).thenReturn(10);

        cartService.updateQuantity(
                1L,
                1L,
                request
        );

        verify(cartItem)
                .changeQuantity(5);
    }

    /**
     * 본인 장바구니가 아닐시
     */
    @Test
    @DisplayName("다른 사람 장바구니 수정 불가")
    void updateQuantity_forbidden() {

        Cart myCart = mock(Cart.class);
        Cart anotherCart = mock(Cart.class);

        CartItem cartItem = mock(CartItem.class);

        UpdateCartItemQuantityRequest request =
                new UpdateCartItemQuantityRequest();

        ReflectionTestUtils.setField(
                request,
                "quantity",
                5
        );

        when(myCart.getId()).thenReturn(1L);
        when(anotherCart.getId()).thenReturn(2L);

        when(cartRepository.findByMemberId(1L))
                .thenReturn(Optional.of(myCart));

        when(cartItemRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(cartItem));

        when(cartItem.getCart())
                .thenReturn(anotherCart);

        assertThrows(
                ServiceException.class,
                () -> cartService.updateQuantity(
                        1L,
                        1L,
                        request
                )
        );
    }

    @Test
    @DisplayName("장바구니 조회 성공: 장바구니 품목 전체와 총액 계산 메서드가 정확히 호출된다.")
    void getCart_Success() {
        // given
        Cart cart = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);
        Product product = mock(Product.class);

        when(cart.getId()).thenReturn(10L);
        when(product.getId()).thenReturn(5L);
        when(product.getName()).thenReturn("에어팟");
        when(product.getPrice()).thenReturn(200000L);
        when(cartItem.getId()).thenReturn(100L);
        when(cartItem.getProduct()).thenReturn(product);
        when(cartItem.getQuantity()).thenReturn(2);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndDeletedAtIsNull(10L)).thenReturn(List.of(cartItem));

        // when
        CartResponse response = cartService.getCart(1L);

        // then
        assertNotNull(response);
        assertEquals(10L, response.getCartId());
        assertEquals(1, response.getCartItems().size());
        assertEquals(400000, response.getTotalAmount()); // 200,000 * 2 = 400,000 원천 정합성 확인
    }

    @Test
    @DisplayName("장바구니 조회 실패: 장바구니 자체가 존재하지 않으면 CART_ITEM_NOT_FOUND 예외가 발생한다.")
    void getCart_Fail_NotFound() {
        // given
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ServiceException.class, () -> cartService.getCart(1L));
    }

    @Test
    @DisplayName("장바구니 상품 개별 삭제 성공: 본인 소유 장바구니임이 검증되면 softDelete 메서드가 트리거된다.")
    void deleteCartItem_Success() {
        // given
        Cart cart = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);

        when(cart.getId()).thenReturn(5L);
        when(cartItem.getCart()).thenReturn(cart);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(cartItem));

        // when
        cartService.deleteCartItem(1L, 100L);

        // then
        verify(cartItem, times(1)).softDelete();
    }

    @Test
    @DisplayName("장바구니 상품 개별 삭제 실패: 삭제를 요청한 상품이 다른 사람의 장바구니 품목이라면 FORBIDDEN 예외가 발생한다.")
    void deleteCartItem_Fail_Forbidden() {
        // given
        Cart myCart = mock(Cart.class);
        Cart anotherCart = mock(Cart.class);
        CartItem cartItem = mock(CartItem.class);

        when(myCart.getId()).thenReturn(1L);
        when(anotherCart.getId()).thenReturn(2L);
        when(cartItem.getCart()).thenReturn(anotherCart); // 남의 카트 매핑

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(myCart));
        when(cartItemRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(cartItem));

        // when & then
        assertThrows(ServiceException.class, () -> cartService.deleteCartItem(1L, 100L));
        verify(cartItem, never()).softDelete();
    }

    @Test
    @DisplayName("장바구니 상품 개별 삭제 실패: 존재하지 않는 품목 번호 상신 시 CART_ITEM_NOT_FOUND 예외가 발생한다.")
    void deleteCartItem_Fail_NotFound() {
        // given
        Cart cart = mock(Cart.class);
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ServiceException.class, () -> cartService.deleteCartItem(1L, 100L));
    }

    @Test
    @DisplayName("장바구니 전체 비우기 성공: 장바구니에 담긴 모든 CartItem이 순회하며 softDelete 처리된다.")
    void clearCart_Success() {
        // given
        Cart cart = mock(Cart.class);
        CartItem item1 = mock(CartItem.class);
        CartItem item2 = mock(CartItem.class);
        List<CartItem> itemList = new ArrayList<>(List.of(item1, item2));

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartAndDeletedAtIsNull(cart)).thenReturn(itemList);

        // when
        cartService.clearCart(1L);

        // then
        verify(item1, times(1)).softDelete();
        verify(item2, times(1)).softDelete();
    }
}
