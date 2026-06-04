package com.team11.jojopay.domain.order.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.order.dto.request.OrderCreateRequest;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.OrderPreviewResponse;
import com.team11.jojopay.domain.order.dto.response.OrderResponse;
import com.team11.jojopay.domain.order.dto.response.PreviewItem;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 주문서 미리보기 정보를 제공합니다.
     * 결제 직전 장바구니 데이터를 바탕으로 현재 상품의 실시간 가격과 재고를 반영하여 결제 예상 정보를 산출합니다.
     * DB 저장이 발생하지 않는 읽기 전용(Read-Only) 트랜잭션으로 동작합니다.
     *
     * @param memberId JWT 기반으로 인증된 회원 ID (본인 소유 장바구니 검증용)
     * @param request 선택된 장바구니 아이템 ID 목록이 포함된 요청 DTO
     * @return 실시간 상품명, 현재가, 수량, 결제 예상 합계 금액이 포함된 응답 DTO
     */
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(Long memberId, OrderPreviewRequest request) {
        List<CartItem> cartItems;

        // 1. 선택적 조회 로직: 요청 파라미터가 비어있으면 장바구니 전체를, 있으면 선택된 항목만 필터링합니다.
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            cartItems = cartItemRepository.findAllByMemberId(memberId);
        } else {
            cartItems = cartItemRepository.findAllByIdInAndMemberId(request.getCartItemIds(), memberId);
        }

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("결제할 장바구니 상품이 존재하지 않습니다.");
        }

        // 2. 실시간 데이터 기반 결제 예상 총액 산출
        long totalAmount = 0;
        List<PreviewItem> previewItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            // 장바구니 아이템에 매핑된 실제 상품 정보를 실시간으로 조회합니다.
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 정보가 존재하지 않습니다."));

            // 3. 상품 유효성 검증: 품절이거나 판매 중단된 상태인지 확인합니다.
            if (!product.isAvailable() || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("판매가 중단되었거나 재고가 부족한 상품이 포함되어 있습니다: " + product.getName());
            }

            // 스냅샷 생성 전이므로 상품의 실시간 현재가를 반영하여 금액을 계산합니다.
            long itemTotalPrice = product.getPrice() * cartItem.getQuantity();
            totalAmount += itemTotalPrice;

            // 응답용 아이템 DTO 구성
            previewItems.add(PreviewItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice()) // 실시간 현재가 반영
                    .quantity(cartItem.getQuantity())
                    .build());
        }

        return OrderPreviewResponse.builder()
                .items(previewItems)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 사용자의 장바구니 항목을 기반으로 새로운 주문을 생성합니다.
     *
     * <p>이 메서드는 다음의 비즈니스 로직을 단일 트랜잭션으로 원자적으로 처리합니다:
     * <ol>
     * <li>요청된 장바구니 아이템 식별자를 기반으로 회원 소유의 장바구니 항목을 조회합니다.</li>
     * <li>각 상품의 재고를 검증하고 선차감합니다. (재고 부족 시 예외 발생 및 전체 롤백)</li>
     * <li>주문 생성 시점의 상품명과 가격을 스냅샷으로 저장합니다. (위변조 방지를 위해 서버단 가격 사용)</li>
     * <li>총 결제 금액을 계산하고 포인트 사용량을 검증합니다.</li>
     * <li>주문(Order) 및 결제(Payment) 대기 상태의 레코드를 생성합니다.</li>
     * <li>결제가 완료된 장바구니 항목을 삭제합니다.</li>
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

        List<CartItem> cartItems = cartItemRepository.findAllByIdInAndMemberId(request.getCartItemIds(), memberId);
        if (cartItems.isEmpty()) {
            // IllegalArgumentException 대신 팀 컨벤션인 CustomException 사용
            throw new ServiceException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        long totalAmount = 0;

        // 정적 팩토리 메서드(createOrder) 사용
        Order order = Order.createOrder(memberId, generateOrderNumber(), totalAmount, request.getUsedPoint());

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_STOCK); // 재고 부족 즉시 예외
            }
            product.decreaseStock(cartItem.getQuantity()); // 명확한 의도의 업데이트 메서드 사용

            long itemTotalPrice = product.getPrice() * cartItem.getQuantity();
            totalAmount += itemTotalPrice;

            // 정적 팩토리 메서드 사용
            OrderItem orderItem = OrderItem.createOrderItem(
                    product.getId(), product.getName(), product.getPrice(), cartItem.getQuantity()
            );
            order.addOrderItem(orderItem);
        }

        order.updateTotalAmount(totalAmount); // Setter 대신 명확한 네이밍의 메서드 사용

        long pgRealAmount = totalAmount - request.getUsedPoint();
        if (pgRealAmount < 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        orderRepository.save(order);

        String portonePaymentId = "pay-" + UUID.randomUUID().toString();

        // 정적 팩토리 메서드 사용
        Payment payment = Payment.createPayment(
                order,
                portonePaymentId,
                totalAmount,
                request.getUsedPoint()
        );
        paymentRepository.save(payment);

        cartItemRepository.deleteAll(cartItems);

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .totalAmount(totalAmount)
                .usedPoint(request.getUsedPoint())
                .pgRealAmount(pgRealAmount)
                .portonePaymentId(portonePaymentId)
                .build();
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }
}