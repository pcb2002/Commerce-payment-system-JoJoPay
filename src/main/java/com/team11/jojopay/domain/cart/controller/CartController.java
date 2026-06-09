package com.team11.jojopay.domain.cart.controller;


import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.dto.request.UpdateCartItemQuantityRequest;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
import com.team11.jojopay.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 담기
    @PostMapping("/items")
    public ResponseEntity<Void> addCartItem(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody AddCartItemRequest request) {
        cartService.addCartItem(memberId, request);
        return ResponseEntity.ok().build();
    }
    // 장바구니 조회
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal Long memberId) {
        CartResponse response = cartService.getCart(memberId);
        return ResponseEntity.ok(response);
    }
    // 장바구니 상품 수량 변경
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateQuantity(
            @AuthenticationPrincipal Long memberId, @PathVariable Long cartItemId, @Valid
            @RequestBody UpdateCartItemQuantityRequest request) {
        cartService.updateQuantity(memberId, cartItemId, request);
        return ResponseEntity.ok().build();
    }
    // 장바구니 상품 삭제
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(
            @AuthenticationPrincipal Long memberId, @PathVariable Long cartItemId) {
        cartService.deleteCartItem(memberId, cartItemId);
        return ResponseEntity.ok().build();
    }
    // 장바구니 전체 비우기
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal Long memberId) {
        cartService.clearCart(memberId);
        return ResponseEntity.ok().build();
    }

}
