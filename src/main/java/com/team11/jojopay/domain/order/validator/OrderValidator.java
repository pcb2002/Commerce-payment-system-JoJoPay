package com.team11.jojopay.domain.order.validator;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.cartitem.entity.CartItem;
import com.team11.jojopay.domain.cartitem.repository.CartItemRepository;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    /**
     * 장바구니 아이템 유효성 및 본인 소유 검증
     */
    public List<CartItem> validateAndGetCartItems(List<Long> cartItemIds, Long memberId) {
        List<CartItem> cartItems = cartItemRepository.findAllByIdInAndMemberId(cartItemIds, memberId);
        if (cartItems.isEmpty()) {
            throw new ServiceException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return cartItems;
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
}