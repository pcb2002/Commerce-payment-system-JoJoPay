package com.team11.jojopay.domain.product.service;


import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.product.dto.request.ProductSearchRequest;
import com.team11.jojopay.domain.product.dto.response.ProductDetailResponse;
import com.team11.jojopay.domain.product.dto.response.ProductListResponse;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;


    // Product 생성 헬퍼 메서드

    private Product createProduct() {

        return Product.create(
                "맥북",
                1000L,
                10,
                "설명",
                ProductStatus.ON_SALE,
                Category.ELECTRONICS
        );
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getDetailProduct_success() {

        // given
        Product product = createProduct();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response =
                productService.getDetailProduct(1L);

        // then
        assertNotNull(response);

        verify(productRepository)
                .findById(1L);
    }

    @Test
    @DisplayName("상품 단건 조회 실패")
    void getDetailProduct_fail() {

        // given
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                ServiceException.class,
                () -> productService.getDetailProduct(999L)
        );
    }

    @Test
    @DisplayName("재고 복구 성공")
    void increaseStock_success() {

        // given
        Product product = createProduct();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // when
        productService.increaseStock(
                1L,
                5
        );

        // then
        assertEquals(
                15,
                product.getStock()
        );
    }

    @Test
    @DisplayName("상품 없음 - 재고 복구 실패")
    void increaseStock_fail() {

        // given
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                ServiceException.class,
                () -> productService.increaseStock(
                        1L,
                        5
                )
        );
    }

    /**
     * ProductSearchRequest 생성 헬퍼 메서드
     * Request DTO에 생성자가 없으므로 Reflection 사용
     */

    private ProductSearchRequest createSearchRequest(
            String sort
    ) {

        ProductSearchRequest request =
                new ProductSearchRequest();

        ReflectionTestUtils.setField(
                request,
                "page",
                0
        );

        ReflectionTestUtils.setField(
                request,
                "size",
                10
        );

        ReflectionTestUtils.setField(
                request,
                "sort",
                sort
        );

        return request;
    }

    @Test
    @DisplayName("상품 목록 조회 성공")
    void getProductList_success() {

        // given
        Product product = createProduct();

        ProductSearchRequest request =
                createSearchRequest("latest");

        Page<Product> page =
                new PageImpl<>(
                        List.of(product)
                );

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);

        // when
        Page<ProductListResponse> response =
                productService.getProductList(request);

        // then
        assertEquals(
                1,
                response.getContent().size()
        );
    }


    @Test
    @DisplayName("가격 오름차순 정렬")
    void getProductList_priceAsc() {

        ProductSearchRequest request =
                createSearchRequest("priceAsc");

        when(productRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of())
        );

        productService.getProductList(request);

        verify(productRepository)
                .findAll(
                        any(Specification.class),
                        argThat((Pageable pageable) ->
                                pageable.getSort()
                                        .getOrderFor("price")
                                        .getDirection()
                                        == Sort.Direction.ASC
                        )
                );

    }

}
