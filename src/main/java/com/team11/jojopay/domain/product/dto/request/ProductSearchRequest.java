package com.team11.jojopay.domain.product.dto.request;


import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSearchRequest {

    // 목록 조회 검색 조건 dto

    // 카테고리 필터
    private Category category;

    // 최소 가격
    @Min(value = 0, message = "최소 가격은 0원 이상이여야 합니다.")
    private Integer minPrice;

    // 최대 가격
    @Max(value = 1000000, message = "최대 가격은 1,000,000원 이상이여야 합니다.")
    private Integer maxPrice;

    // 판매 상태
    private ProductStatus status;

    // 페이지 번호
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    // 페이지 크기
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    // 정렬 조건
    // ex) latest, priceAsc, priceDesc
    private String sort = "latest";


}
