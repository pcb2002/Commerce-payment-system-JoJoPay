package com.team11.jojopay.domain.order.service;

import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.OrderPreviewResponse;
import com.team11.jojopay.domain.order.dto.response.PreviewItem;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 도메인의 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

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
        int totalAmount = 0;
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
            int itemTotalPrice = product.getPrice() * cartItem.getQuantity();
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
}