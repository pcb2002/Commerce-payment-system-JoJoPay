package com.team11.jojopay.domain.product.dto.response;


import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductListResponse {

    // 상품 목록 조회 응답 dto
    // 여기선 상품 상세 설명은 제외함

    private Long productId;

    private String productName;

    private Long price;

    private Integer stockQuantity;

    private Category category;

    private ProductStatus status;

    // entity → dto 변환을 위해 from 사용
    public static ProductListResponse from(Product product) {

        return ProductListResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStock())
                .category(product.getCategory())
                .status(product.getStatus())
                .build();
    }
}
