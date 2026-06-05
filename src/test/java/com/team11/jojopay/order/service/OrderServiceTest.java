package com.team11.jojopay.order.service;

package com.team11.jojopay.domain.order.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.order.dto.request.OrderCreateRequest;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.OrderPreviewResponse;
import com.team11.jojopay.domain.order.dto.response.OrderResponse;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.order.service.OrderService;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderValidator orderValidator;

    @Test
    @DisplayName("[미리보기] 장바구니 데이터를 바탕으로 실시간 총액을 정확히 산출한다.")
    void preview_Success() {
        // given
        Long memberId = 1L;
        OrderPreviewRequest request = new OrderPreviewRequest(List.of(1L));

        CartItem cartItem = mock(CartItem.class);
        given(cartItem.getProductId()).willReturn(100L);
        given(cartItem.getQuantity()).willReturn(2); // 2개 담음

        Product product = mock(Product.class);
        given(product.getPrice()).willReturn(50000L); // 단가 5만 원

        given(orderValidator.getCartItemsForPreview(request.getCartItemIds(), memberId))
                .willReturn(List.of(cartItem));
        given(orderValidator.validateAndGetProduct(100L))
                .willReturn(product);

        // when
        OrderPreviewResponse response = orderService.preview(memberId, request);

        // then
        verify(product).validateOrderable(2); // 엔티티 검증 메서드 호출 여부 확인
        assertThat(response.getTotalAmount()).isEqualTo(100000L); // 5만 원 * 2개 = 10만 원
    }

    @Test
    @DisplayName("[주문 생성] 유효한 요청 시 주문, 결제 레코드가 생성되고 장바구니가 비워진다.")
    void createOrder_Success() {
        // given
        Long memberId = 1L;
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 10000L); // 1만 원 포인트 사용

        CartItem cartItem = mock(CartItem.class);
        given(cartItem.getProductId()).willReturn(100L);
        given(cartItem.getQuantity()).willReturn(2);

        Product product = mock(Product.class);
        given(product.getId()).willReturn(100L);
        given(product.getName()).willReturn("테스트 상품");
        given(product.getPrice()).willReturn(50000L); // 총액 10만 원

        given(orderValidator.validateAndGetCartItems(request.getCartItemIds(), memberId))
                .willReturn(List.of(cartItem));
        given(orderValidator.validateAndGetProduct(100L)).willReturn(product);

        // when
        OrderResponse response = orderService.createOrder(memberId, request);

        // then
        verify(product).decreaseStock(2); // 재고 차감 메서드 호출 확인
        verify(orderRepository).save(any(Order.class)); // 주문 저장 확인
        verify(paymentRepository).save(any(Payment.class)); // 결제 저장 확인
        verify(cartItemRepository).deleteAll(anyList()); // 장바구니 비우기 확인

        // 총액 10만 원 - 포인트 1만 원 = PG 결제 금액 9만 원
        assertThat(response.getPgRealAmount()).isEqualTo(90000L);
        assertThat(response.getPortonePaymentId()).startsWith("pay-"); // 식별자 채번 확인
    }

    @Test
    @DisplayName("[주문 생성 예외] 사용하려는 포인트가 총 결제 금액을 초과하면 예외가 발생한다.")
    void createOrder_Fail_InvalidPointAmount() {
        // given
        Long memberId = 1L;
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 150000L); // 15만 원 포인트 사용 시도

        CartItem cartItem = mock(CartItem.class);
        given(cartItem.getProductId()).willReturn(100L);
        given(cartItem.getQuantity()).willReturn(2);

        Product product = mock(Product.class);
        given(product.getPrice()).willReturn(50000L); // 총액 10만 원

        given(orderValidator.validateAndGetCartItems(request.getCartItemIds(), memberId))
                .willReturn(List.of(cartItem));
        given(orderValidator.validateAndGetProduct(100L)).willReturn(product);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(memberId, request))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_POINT_AMOUNT); // pgRealAmount < 0 검증

        // 예외 발생 시 save 및 deleteAll이 호출되지 않았는지 확인 (트랜잭션 롤백 방어선)
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("✅ [주문 취소] 주문 취소 시 재고가 복구되고 결제 상태가 변경된다.")
    void cancelOrder_Success() {
        // given
        Long memberId = 1L;
        String orderNumber = "ORD-2026-TEST";

        Order order = mock(Order.class);
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getProductId()).willReturn(100L);
        given(orderItem.getQuantity()).willReturn(3);
        given(order.getOrderItems()).willReturn(List.of(orderItem));

        Product product = mock(Product.class);
        Payment payment = mock(Payment.class);

        // Validator 모킹
        given(orderValidator.validateAndGetOrder(orderNumber, memberId)).willReturn(order);
        given(orderValidator.validateAndGetProductWithLock(100L)).willReturn(product);
        given(orderValidator.validateAndGetPayment(order)).willReturn(payment);

        // when
        orderService.cancelOrder(memberId, orderNumber);

        // then
        verify(order).cancelOrder(); // 도메인 로직: 주문 상태 취소 확인
        verify(product).increaseStock(3); // 도메인 로직: 재고 3개 복구 확인
        verify(payment).cancel(); // 도메인 로직: 결제 상태 취소 확인
    }
}