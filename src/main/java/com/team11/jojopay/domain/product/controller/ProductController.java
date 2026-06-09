package com.team11.jojopay.domain.product.controller;

import com.team11.jojopay.domain.product.dto.request.ProductSearchRequest;
import com.team11.jojopay.domain.product.dto.response.ProductDetailResponse;
import com.team11.jojopay.domain.product.dto.response.ProductListResponse;
import com.team11.jojopay.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    // 속성
    private final ProductService productService;


    // 상품 목록 조회
    // 흐름 → 요청 →  ProductSearchRequest로 자동 매핑 → Service에서 Pageable 생성
    @GetMapping
    public Page<ProductListResponse> getProducts(
            @Valid ProductSearchRequest request
    ) {

        // 서비스 호출
        return productService.getProductList(request);
    }



    // 상품 단건 조회
    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(
            @PathVariable Long productId
    ) {

        // 서비스 호출
        return productService.getDetailProduct(productId);
    }

}
