package com.team11.jojopay.domain.product.dto.response;


import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDetailResponse {

    // 상품 단건 조회용 dto
    // 여기선 상품 상세 설명 포함

    private Long productId;

    private String productName;

    private Long price;

    private Integer stockQuantity;

    private String description;

    private Category category;

    private ProductStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;



    //entity → dto 변환을 위해 from 사용
    public static ProductDetailResponse from(Product product) {

        return ProductDetailResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStock())
                .description(product.getDescription())
                .category(product.getCategory())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
