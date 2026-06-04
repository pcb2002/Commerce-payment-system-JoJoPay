package com.team11.jojopay.domain.order.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.order.dto.request.OrderCreateRequest;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.*;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.order.validator.OrderValidator;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문 도메인의 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderValidator orderValidator;

    /**
     * 주문서 미리보기 정보를 제공합니다.
     * * <p>결제 직전 장바구니 데이터를 바탕으로 실시간 가격과 재고를 검증하여 예상 결제액을 산출합니다.
     * DB 저장이 발생하지 않으며, 다음의 흐름으로 진행됩니다:
     * <ol>
     * <li>{@code OrderValidator}를 통해 대상 장바구니 항목을 필터링 및 조회합니다.</li>
     * <li>각 상품({@code Product}) 객체에게 판매 가능 여부와 재고 검증을 위임합니다.</li>
     * <li>실시간 현재가를 바탕으로 예상 총 결제 금액을 산출하고 응답 DTO를 구성합니다.</li>
     * </ol>
     *
     * @param memberId 현재 인증된 사용자의 식별자
     * @param request  선택된 장바구니 아이템 ID 목록 (null/empty 일 경우 전체 조회)
     * @return 실시간 상품 정보 및 결제 예상 총액이 포함된 응답 DTO
     */
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(Long memberId, OrderPreviewRequest request) {

        // 1. 조회 및 검증 위임 (Validator)
        List<CartItem> cartItems = orderValidator.getCartItemsForPreview(request.getCartItemIds(), memberId);

        long totalAmount = 0L;
        List<OrderPreviewResponse.PreviewItemResponse> previewItems = new ArrayList<>();

        // 2. 상품별 실시간 검증 및 금액 합산
        for (CartItem cartItem : cartItems) {
            Product product = orderValidator.validateAndGetProduct(cartItem.getProductId());

            // 객체 지향적 검증 (Entity 스스로 검증)
            product.validateOrderable(cartItem.getQuantity());

            // 총액 누적
            totalAmount += (product.getPrice() * cartItem.getQuantity());

            // DTO 조립 위임
            previewItems.add(OrderPreviewResponse.PreviewItemResponse.of(product, cartItem.getQuantity()));
        }

        // 3. 최종 응답 반환
        return OrderPreviewResponse.of(previewItems, totalAmount);
    }

    /**
     * 사용자의 장바구니 항목을 기반으로 새로운 주문을 생성합니다.
     * * <p>이 메서드는 주문 생성의 전체 흐름을 제어하는 오케스트레이터(Orchestrator) 역할을 수행하며,
     * 세부적인 검증 및 계산 로직은 Validator 컴포넌트와 도메인 엔티티에 위임하여
     * 단일 트랜잭션 내에서 다음 로직을 원자적으로 처리합니다:
     * <ol>
     * <li>{@code OrderValidator}를 통해 회원의 장바구니 항목 유효성을 조회 및 검증합니다.</li>
     * <li>상품({@code Product}) 엔티티 스스로 재고를 검증하고 선차감하도록 위임합니다. (재고 부족 시 예외 발생 및 전체 롤백)</li>
     * <li>주문 생성 시점의 상품명과 가격을 스냅샷으로 생성하여 주문 항목({@code OrderItem})에 추가합니다.</li>
     * <li>주문({@code Order}) 엔티티 스스로 총 결제 금액을 산출하고 포인트 사용 초과 여부를 검증하도록 위임합니다.</li>
     * <li>주문 및 결제 대기 상태의 레코드를 영속화하고, PG사 연동을 위한 식별자를 채번합니다.</li>
     * <li>결제가 진행될 장바구니 항목을 삭제합니다.</li>
     * </ol>
     *
     * @param memberId 현재 인증된 사용자의 식별자 (JWT에서 추출)
     * @param request  주문 생성에 필요한 요청 정보 (장바구니 아이템 ID 목록, 사용 포인트)
     * @return 생성된 주문의 식별 번호, 총액, PG사 결제 요청 금액, 외부 연동(PortOne) 식별자가 포함된 응답 객체
     *
     * @throws ServiceException 장바구니 항목을 찾을 수 없는 경우 ({@link ErrorCode#CART_ITEM_NOT_FOUND})
     * @throws ServiceException 상품 정보를 찾을 수 없는 경우 ({@link ErrorCode#PRODUCT_NOT_FOUND})
     * @throws ServiceException 상품의 재고가 부족한 경우 ({@link ErrorCode#INSUFFICIENT_STOCK})
     * @throws ServiceException 사용 포인트가 총 결제 금액을 초과하는 경우 ({@link ErrorCode#INVALID_POINT_AMOUNT})
     */
    @Transactional // 변경이 일어나는 메서드 필수
    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {

        // 1. 검증 위임: 장바구니 유효성 검사
        List<CartItem> cartItems = orderValidator.validateAndGetCartItems(request.getCartItemIds(), memberId);

        // 2. 주문 엔티티 생성 (초기화)
        long totalAmount = 0;
        Order order = Order.createOrder(memberId, generateOrderNumber(), totalAmount, request.getUsedPoint());

        // 3. 비즈니스 로직 수행
        for (CartItem cartItem : cartItems) {
            Product product = orderValidator.validateAndGetProduct(cartItem.getProductId());

            product.decreaseStock(cartItem.getQuantity()); // 엔티티 내부에서 재고 검증 및 차감

            long itemTotalPrice = product.getPrice() * cartItem.getQuantity();
            totalAmount += itemTotalPrice;

            // 정적 팩토리 메서드 사용
            OrderItem orderItem = OrderItem.createOrderItem(
                    product.getId(), product.getName(), product.getPrice(), cartItem.getQuantity()
            );
            order.addOrderItem(orderItem);
        }
        // 4. 엔티티에 계산 및 검증 위임 (포인트 초과 사용 등)
        order.calculateAndValidateAmount(totalAmount, request.getUsedPoint());

        // 5. DB 저장 및 연관 레코드 생성
        orderRepository.save(order);

        String portonePaymentId = "pay-" + UUID.randomUUID();

        long pgRealAmount = totalAmount - request.getUsedPoint();
        if (pgRealAmount < 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        // 정적 팩토리 메서드 사용
        Payment payment = Payment.createPayment(
                order,
                portonePaymentId,
                totalAmount,
                request.getUsedPoint()
        );
        paymentRepository.save(payment);

        cartItemRepository.deleteAll(cartItems);

        return OrderResponse.of(order, payment, pgRealAmount);
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    /**
     * 내 주문 내역 목록을 페이징하여 조회합니다. (최신순 정렬)
     * * @param memberId 회원 ID
     * @param pageable 페이징 정보 (page, size)
     * @return 페이징 처리된 주문 목록 DTO
     */
    @Transactional(readOnly = true)
    public Page<OrderListItemResponse> getMyOrders(Long memberId, Pageable pageable) {
        // Repository에서 Pageable을 통해 최신순 정렬 및 페이징된 데이터를 가져옵니다.
        Page<Order> orders = orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        return orders.map(OrderListItemResponse::from);
    }

    /**
     * 특정 주문의 상세 정보(상품 스냅샷, 결제 정보 포함)를 조회합니다.
     * * @param memberId 회원 ID (타인 주문 조회 방지 검증용)
     * @param orderNumber 주문 번호
     * @return 주문 상세 내역 DTO
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, String orderNumber) {

        // 1. 주문 및 주문 상품(스냅샷) 조회
        // @EntityGraph가 적용된 쿼리를 사용하여 N+1 문제 없이 한 번의 조인(Fetch Join)으로 가져옵니다.
        Order order = orderRepository.findWithOrderItemsByOrderNumberAndMemberId(orderNumber, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 결제 내역 조회 (결제 담당자가 만든 Payment 엔티티 활용)
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        return OrderDetailResponse.of(order, payment);
    }
}