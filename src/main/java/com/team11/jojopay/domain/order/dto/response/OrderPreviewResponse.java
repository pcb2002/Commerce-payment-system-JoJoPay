package com.team11.jojopay.domain.order.dto.response;

import com.team11.jojopay.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 주문서 미리보기 응답 DTO입니다.
 * DB 저장 없이 실시간 상품 정보(스냅샷 저장 전 최신가)와 예상 결제 총액을 반환합니다.
 */
@Getter
@Builder
public class OrderPreviewResponse {

    private List<PreviewItemResponse> items;
    private Long totalAmount; // Integer -> Long 컨벤션 적용

    // 팩토리 메서드: List와 총액을 받아 조립
    public static OrderPreviewResponse of(List<PreviewItemResponse> items, long totalAmount) {
        return OrderPreviewResponse.builder()
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    @Getter
    @Builder
    public static class PreviewItemResponse {
        private Long productId;
        private String productName;
        private Long price;
        private Integer quantity;

        // 팩토리 메서드: 상품 엔티티와 수량을 받아 조립
        public static PreviewItemResponse of(Product product, Integer quantity) {
            return PreviewItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice()) // 실시간 현재가
                    .quantity(quantity)
                    .build();
        }
    }
}