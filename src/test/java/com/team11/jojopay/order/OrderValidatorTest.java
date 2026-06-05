package com.team11.jojopay.order;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @InjectMocks
    private OrderValidator orderValidator;

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("✅ [성공] 장바구니 아이템 검증 - 아이템이 존재할 경우 리스트를 반환한다.")
    void validateAndGetCartItems_Success() {
        // given
        Long memberId = 1L;
        List<Long> cartItemIds = List.of(1L, 2L);
        List<CartItem> expectedItems = List.of(mock(CartItem.class), mock(CartItem.class));

        given(cartItemRepository.findAllByIdInAndMemberId(cartItemIds, memberId))
                .willReturn(expectedItems);

        // when
        List<CartItem> result = orderValidator.validateAndGetCartItems(cartItemIds, memberId);

        // then
        assertThat(result).hasSize(2).isEqualTo(expectedItems);
    }

    @Test
    @DisplayName("🚨 [실패] 장바구니 아이템 검증 - 아이템이 없거나 본인 소유가 아니면 예외를 발생시킨다.")
    void validateAndGetCartItems_Fail_NotFound() {
        // given
        Long memberId = 1L;
        List<Long> cartItemIds = List.of(1L, 2L);

        given(cartItemRepository.findAllByIdInAndMemberId(cartItemIds, memberId))
                .willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> orderValidator.validateAndGetCartItems(cartItemIds, memberId))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("✅ [성공] 미리보기 장바구니 조회 - 선택 ID가 없을 경우 회원 전체 장바구니를 반환한다.")
    void getCartItemsForPreview_Success_EmptyIds() {
        // given
        Long memberId = 1L;
        List<CartItem> expectedItems = List.of(mock(CartItem.class));

        given(cartItemRepository.findAllByMemberId(memberId))
                .willReturn(expectedItems);

        // when
        List<CartItem> result = orderValidator.getCartItemsForPreview(null, memberId); // ID 리스트 null 전달

        // then
        assertThat(result).isEqualTo(expectedItems);
    }

    @Test
    @DisplayName("✅ [성공] 단건 주문 조회 - 본인 소유의 주문일 경우 정상 반환한다.")
    void validateAndGetOrder_Success() {
        // given
        String orderNumber = "ORD-TEST";
        Long memberId = 1L;
        Order expectedOrder = mock(Order.class);

        given(orderRepository.findWithOrderItemsByOrderNumberAndMemberId(orderNumber, memberId))
                .willReturn(Optional.of(expectedOrder));

        // when
        Order result = orderValidator.validateAndGetOrder(orderNumber, memberId);

        // then
        assertThat(result).isEqualTo(expectedOrder);
    }

    @Test
    @DisplayName("🚨 [실패] 단건 주문 조회 - 주문이 없거나 타인 소유일 경우 예외를 발생시킨다.")
    void validateAndGetOrder_Fail_NotFound() {
        // given
        String orderNumber = "ORD-TEST";
        Long memberId = 2L; // 타인 접근 시도

        given(orderRepository.findWithOrderItemsByOrderNumberAndMemberId(orderNumber, memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderValidator.validateAndGetOrder(orderNumber, memberId))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("✅ [성공] 페이징 주문 목록 조회 - 회원의 최신 주문 목록을 반환한다.")
    void getOrdersByMemberId_Success() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> expectedPage = new PageImpl<>(List.of(mock(Order.class)));

        given(orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable))
                .willReturn(expectedPage);

        // when
        Page<Order> result = orderValidator.getOrdersByMemberId(memberId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("🚨 [실패] 상품 비관적 락 조회 - 상품이 존재하지 않으면 예외를 발생시킨다.")
    void validateAndGetProductWithLock_Fail_NotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findByIdWithLock(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderValidator.validateAndGetProductWithLock(productId))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }
}