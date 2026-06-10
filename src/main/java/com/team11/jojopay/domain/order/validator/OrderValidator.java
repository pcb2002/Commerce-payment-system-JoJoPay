package com.team11.jojopay.domain.order.validator;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.entity.OrderItem;
import com.team11.jojopay.domain.order.reopsitory.OrderItemRepository;
import com.team11.jojopay.domain.order.reopsitory.OrderRepository;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;

    /**
     * 장바구니 아이템 유효성 및 본인 소유 검증
     */
    public List<CartItem> validateAndGetCartItems(List<Long> cartItemIds, Long memberId) {
        // 1. ID 정렬 (데드락 방지)
        List<Long> sortedIds = new ArrayList<>(cartItemIds);
        sortedIds.sort(Long::compareTo);

        // 2. 데이터 조회
        List<CartItem> cartItems = cartItemRepository.findAllByIdInAndMemberId(sortedIds, memberId);

        if (cartItems.isEmpty()) {
            throw new ServiceException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // 3. 조회된 결과를 가변 리스트로 복사 후 정렬 (에러 해결!)
        List<CartItem> mutableCartItems = new ArrayList<>(cartItems);
        mutableCartItems.sort(Comparator.comparing(CartItem::getProductId));

        return mutableCartItems;
    }

    /**
     * 상품 존재 여부 조회
     */
    public Product validateAndGetProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 선택된 장바구니 ID가 있으면 해당 항목만, 없으면 전체 장바구니 항목을 조회하고 검증합니다.
     */
    public List<CartItem> getCartItemsForPreview(List<Long> cartItemIds, Long memberId) {
        List<CartItem> cartItems;

        if (cartItemIds == null || cartItemIds.isEmpty()) {
            cartItems = cartItemRepository.findAllByMemberId(memberId);
        } else {
            cartItems = cartItemRepository.findAllByIdInAndMemberId(cartItemIds, memberId);
        }

        if (cartItems.isEmpty()) {
            throw new ServiceException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return cartItems;
    }

    /**
     * [Reader] 내 주문 내역 목록을 최신순으로 페이징하여 조회합니다.
     */
    public Page<Order> getOrdersByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    /**
     * 본인 소유의 주문인지 검증하고 엔티티를 반환합니다.
     */
    public Order validateAndGetOrder(String orderNumber, Long memberId) {
        return orderRepository.findWithOrderItemsByOrderNumberAndMemberId(orderNumber, memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 해당 주문과 연동된 결제 내역이 있는지 검증하고 반환합니다.
     */
    public Payment validateAndGetPayment(Order order) {
        return paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * [주문 취소용] 재고 복구를 위해 비관적 락(Lock)을 걸어 상품을 조회합니다.
     */
    public Product validateAndGetProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * [환불 서비스용]
     */
    public List<OrderItem> validateAndGetOrderItems(List<Long> orderItemIds, Long memberId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByIdInAndOrderMemberId(orderItemIds, memberId);

        // 요청한 상품 개수와 DB에서 회원이 소유한 것으로 확인된 상품 개수가 다르면 (타인의 상품 ID가 섞여 있으면)
        if (orderItems.size() != orderItemIds.size()) {
            throw new ServiceException(ErrorCode.INVALID_QUANTITY); // 예: 상품 개수 다름
        }

        return orderItems;
    }

    public Member validateAndGetMemberWithLock(Long memberId) {
        return memberRepository.findByIdWithLock(memberId).orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));
    }
}