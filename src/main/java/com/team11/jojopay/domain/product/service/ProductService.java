package com.team11.jojopay.domain.product.service;


import com.team11.jojopay.domain.product.dto.request.ProductSearchRequest;
import com.team11.jojopay.domain.product.dto.response.ProductDetailResponse;
import com.team11.jojopay.domain.product.dto.response.ProductListResponse;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import com.team11.jojopay.domain.product.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    //속성
    private final ProductRepository productRepository;

    // 상품 단건 조회
    public ProductDetailResponse getDetailProduct(Long productId) {

        // 1. DB에서 상품 조회하기
        Product findProduct = productRepository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("상품을 찾을 수 없습니다.")
                );

        // 2. 데이터 준비하기
        Long responseProductId = findProduct.getId();

        String responseProductName = findProduct.getName();

        Integer responsePrice = findProduct.getPrice();

        Integer responseStockQuantity = findProduct.getStockQuantity();

        String responseDescription = findProduct.getDescription();

        // 3. 응답 DTO 만들기
        ProductDetailResponse detailResponse =
                ProductDetailResponse.builder()
                        .productId(responseProductId)
                        .productName(responseProductName)
                        .price(responsePrice)
                        .stockQuantity(responseStockQuantity)
                        .description(responseDescription)
                        .category(findProduct.getCategory())
                        .status(findProduct.getStatus())
                        .createdAt(findProduct.getCreatedAt())
                        .updatedAt(findProduct.getUpdatedAt())
                        .build();

        // 4. 반환하기
        return detailResponse;
    }


    // 상품 목록 조회
    public Page<ProductListResponse> getProductList(
            ProductSearchRequest request
    ) {

        // 1. 정렬 조건 만들기
        Sort sort = createSort(request.getSort());

        // 2. 페이지 정보 만들기
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        // 3. 검색 조건 만들기
        Specification<Product> spec =
                Specification.where(
                                ProductSpecification.hasCategory(
                                        request.getCategory()
                                )
                        )
                        .and(
                                ProductSpecification.hasStatus(
                                        request.getStatus()
                                )
                        )
                        .and(
                                ProductSpecification.minPrice(
                                        request.getMinPrice()
                                )
                        )
                        .and(
                                ProductSpecification.maxPrice(
                                        request.getMaxPrice()
                                )
                        );

        // 4. 상품 목록 조회하기
        Page<Product> findProductPage =
                productRepository.findAll(spec, pageable);

        // 5. DTO 만들기 - stream
        List<ProductListResponse> productResponseList =
                findProductPage.getContent().stream()
                        .map(product ->
                                ProductListResponse.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .price(product.getPrice())
                                        .stockQuantity(product.getStockQuantity())
                                        .category(product.getCategory())
                                        .status(product.getStatus())
                                        .build()
                        )
                        .collect(Collectors.toList());

        // 6. Page 객체로 반환하기
        return new PageImpl<>(
                productResponseList,
                pageable,
                findProductPage.getTotalElements()
        );
    }


    // 정렬 조건 생성
    private Sort createSort(String sort) {

        // 가격 오름차순
        if ("priceAsc".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }

        // 가격 내림차순
        if ("priceDesc".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }

        // 최신순 (기본값)
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
