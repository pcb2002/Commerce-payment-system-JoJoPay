package com.team11.jojopay.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.team11.jojopay.domain.product.dto.request.ProductSearchRequest;
import com.team11.jojopay.domain.product.dto.response.ProductListResponse;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import com.team11.jojopay.domain.product.repository.ProductRepository;
import com.team11.jojopay.domain.product.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 목록 조회 성공 - any() 우회 기법으로 제네릭 경고를 차단한다")
    void getProductList_Success_CleanWarning() {
        // given
        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategory(Category.TOP);
        request.setStatus(ProductStatus.ON_SALE);
        request.setPage(0);
        request.setSize(10);

        Pageable pageable = PageRequest.of(0, 10);
        Product mockProduct = Product.create("티셔츠", 29000L, 50, "설명", ProductStatus.ON_SALE, Category.TOP);
        Page<Product> mockPage = new PageImpl<>(List.of(mockProduct), pageable, 1);

        // -----------------------------------------------------------------
        // Specification 단을 그냥 any() 로 변경하면 Mockito 가 컴파일러 몰래 제네릭을 통과시켜
        // unchecked 경고를 완벽하게 무력화(차단)합니다.
        // -----------------------------------------------------------------
        when(productRepository.findAll(
        org.mockito.ArgumentMatchers.<Specification<Product>>any(), // 컴파일러에게 Specification 타입임을 명확히 인지시킴
        any(Pageable.class)
        )).thenReturn(mockPage);

        // when
        Page<ProductListResponse> result = productService.getProductList(request);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("티셔츠");
    }
}