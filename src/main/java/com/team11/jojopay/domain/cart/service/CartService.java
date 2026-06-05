package com.team11.jojopay.domain.cart.service;


import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.dto.request.UpdateCartItemQuantityRequest;
import com.team11.jojopay.domain.cart.dto.response.CartItemResponse;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
import com.team11.jojopay.domain.cart.entity.Cart;
import com.team11.jojopay.domain.cart.repository.CartRepository;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final ProductRepository productRepository;

    private final MemberRepository memberRepository;

    /**
     * 상품 담기 흐름
     * 1. 회원 장바구니 조회
     * 2. 장바구니 없으면 생성
     * 3. 상품 조회
     * 4. 재고 검증
     * 5. 동일 상품 존재 여부 확인
     * 6. 이미 존재하면 수량 증가
     * 7. 없으면 새 CartItem 생성
     */
    @Transactional
    public void addCartItem(Long memberId, AddCartItemRequest request) {

        /**
         * 회원 조회
         */
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("회원을 찾을 수 없습니다.")
                );

        /**
         * 회원 장바구니 조회
         * 없으면 새 장바구니 생성
         */
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {

                    Cart newCart = new Cart(member);

                    return cartRepository.save(newCart);
                });

        /**
         * 상품 조회
         */
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new IllegalArgumentException("상품을 찾을 수 없습니다.")
                );

        /**
         * 판매 상태 검증
         */
        if (product.getStatus() == ProductStatus.DISCONTINUED) {

            throw new IllegalArgumentException("단종된 상품은 담을 수 없습니다.");
        }

        /**
         * 요청 수량 재고 검증
         */
        if (request.getQuantity() > product.getStock()) {

            throw new IllegalArgumentException("재고를 초과했습니다.");
        }

        /**
         * 동일 상품 장바구니 존재 여부 조회
         * soft delete 되지 않은 데이터만 조회
         */
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductIdAndDeletedAtIsNull(
                        cart.getId(),
                        product.getId()
                )
                .orElse(null);
        /**
         * 이미 존재하는 경우 -> 수량 증가
         */
        if (cartItem != null) {

            int totalQuantity =
                    cartItem.getQuantity()
                            + request.getQuantity();
            /**
             * 합산 후 재고 초과 검증
             */
            if (totalQuantity > product.getStock()) {

                throw new IllegalArgumentException("재고를 초과했습니다.");
            }

            cartItem.addQuantity(request.getQuantity());

            return;
        }

        /**
         * 새 장바구니 상품 생성
         */
        CartItem newCartItem = new CartItem(
                cart,
                product,
                request.getQuantity()
        );
        cartItemRepository.save(newCartItem);
    }

    /**
     * 장바구니 조회
     * 반환 데이터
     * - 장바구니 상품 목록
     * - 각 상품 수량
     * - 전체 합계 금액
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long memberId) {

        /**
         * 회원 장바구니 조회
         */
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니가 존재하지 않습니다.")
                );
        /**
         * 삭제되지 않은 장바구니 상품 조회
         */
        List<CartItem> cartItems =
                cartItemRepository.findByCartIdAndDeletedAtIsNull(
                        cart.getId()
                );

        /**
         * entity -> dto 변환
         */
        List<CartItemResponse> cartItemResponses =
                cartItems.stream()
                        .map(CartItemResponse::from)
                        .toList();

        /**
         * 전체 합계 금액 계산
         */
        int totalAmount =
                cartItems.stream()
                        .mapToInt(cartItem ->
                                cartItem.getProduct().getPrice()
                                        * cartItem.getQuantity()
                        )
                        .sum();

        /**
         * 응답 DTO 반환
         */
        return new CartResponse(
                cart.getId(),
                cartItemResponses,
                totalAmount
        );
    }

    /**
     * 장바구니 상품 수량 변경
     * 검증 내용
     * - 장바구니 상품 존재 여부
     * - 재고 초과 여부
     */
    @Transactional
    public void updateQuantity(
            Long memberId,
            Long cartItemId,
            UpdateCartItemQuantityRequest request
    ) {
        /**
         * 회원 장바구니 조회
         */
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니가 존재하지 않습니다.")
                );

        /**
         * 장바구니 상품 조회
         * 본인 장바구니 상품인지 검증
         */
        CartItem cartItem = cartItemRepository
                .findByIdAndDeletedAtIsNull(cartItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니 상품이 존재하지 않습니다.")
                );

        /**
         * 본인 장바구니 검증
         */
        if (!cartItem.getCart().getId().equals(cart.getId())) {

            throw new IllegalArgumentException("본인 장바구니만 수정 가능합니다.");
        }

        /**
         * 재고 초과 검증
         */
        if (request.getQuantity()
                > cartItem.getProduct().getStock()) {

            throw new IllegalArgumentException("재고를 초과했습니다.");
        }

        /**
         * 수량 변경
         */
        cartItem.changeQuantity(request.getQuantity());
    }

    /**
     * 장바구니 상품 개별 삭제
     * soft delete 방식 사용
     */

    public void deleteCartItem(
            Long memberId,
            Long cartItemId
    ) {
        /**
         * 회원 장바구니 조회
         */
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니가 존재하지 않습니다.")
                );
        /**
         * 장바구니 상품 조회
         */
        CartItem cartItem = cartItemRepository
                .findByIdAndDeletedAtIsNull(cartItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니 상품이 존재하지 않습니다.")
                );

        /**
         * 본인 장바구니 검증
         */
        if (!cartItem.getCart().getId().equals(cart.getId())) {

            throw new IllegalArgumentException("본인 장바구니만 삭제 가능합니다.");
        }

        /**
         * soft delete 처리
         */
        cartItem.softDelete();
    }

    /**
     * 장바구니 전체 비우기
     * soft delete 방식 사용
     */
    @Transactional
    public void clearCart(Long memberId) {
        /**
         * 회원 장바구니 조회
         */
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장바구니가 존재하지 않습니다.")
                );

        /**
         * 삭제되지 않은 장바구니 상품 조회
         */
        List<CartItem> cartItems =
                cartItemRepository.findByCartAndDeletedAtIsNull(cart);

        /**
         * 전체 soft delete 처리
         */
        for (CartItem cartItem : cartItems) {
            cartItem.softDelete();
        }
    }

}
