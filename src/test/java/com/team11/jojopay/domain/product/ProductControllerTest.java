package com.team11.jojopay.domain.product;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.product.controller.ProductController;
import com.team11.jojopay.domain.product.dto.request.ProductSearchRequest;
import com.team11.jojopay.domain.product.dto.response.ProductDetailResponse;
import com.team11.jojopay.domain.product.dto.response.ProductListResponse;
import com.team11.jojopay.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false) // 🟢 시큐리티 필터 간섭 차단 상태 유지
@MockitoBean(types = JpaMetamodelMappingContext.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtProvider jwtProvider; // 가짜 인증 공급자 모킹

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // 가짜 보안 필터 모킹

    // ==========================================
    // 1. 상품 목록 조회 파트 테스트
    // ==========================================
    @Test
    @DisplayName("상품 목록 조회 성공")
    void getProducts_success() throws Exception {
        // given: 빈 리스트를 넣은 정상 PageImpl 구현체를 스텁으로 제공합니다.
        Page<ProductListResponse> page = new PageImpl<>(List.of());
        when(productService.getProductList(any(ProductSearchRequest.class))).thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(productService, times(1)).getProductList(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 필터 조건 전달")
    void getProducts_withCondition() throws Exception {
        // given
        Page<ProductListResponse> page = new PageImpl<>(List.of());
        when(productService.getProductList(any())).thenReturn(page);

        // when & then
        mockMvc.perform(
                        get("/api/v1/products")
                                .param("category", "ELECTRONICS")
                                .param("minPrice", "1000")
                                .param("maxPrice", "5000")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk());

        verify(productService, times(1)).getProductList(any(ProductSearchRequest.class));
    }

    // ==========================================
    // 2. 상품 단건 조회 파트 테스트
    // ==========================================
    @Test
    @DisplayName("상품 단건 조회 성공: 리플렉션을 동원해 컴파일러와 롬복 생성자 장벽을 완전히 분쇄합니다.")
    void getProduct_success() throws Exception {
        // given: "public이 아닙니다", "0-arg 생성자가 없습니다" 에러를 원천 차단하기 위해
        // 자바 리플렉션을 고도화하여 첫 번째로 정의된 생성자를 강제로 열어 인스턴스를 무조건 수립시킵니다.
        ProductDetailResponse response = null;
        try {
            java.lang.reflect.Constructor<?>[] constructors = ProductDetailResponse.class.getDeclaredConstructors();
            if (constructors.length > 0) {
                java.lang.reflect.Constructor<?> targetConstructor = constructors[0];
                targetConstructor.setAccessible(true); // 비공개(private/protected) 장벽을 해제합니다.

                // 생성자 아규먼트 타입 배열을 분석해 알맞은 가짜 기본값 동적 매핑
                Object[] args = new Object[targetConstructor.getParameterCount()];
                Class<?>[] paramTypes = targetConstructor.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    if (paramTypes[i] == Long.class || paramTypes[i] == long.class) args[i] = 1L;
                    else if (paramTypes[i] == Integer.class || paramTypes[i] == int.class) args[i] = 0;
                    else if (paramTypes[i] == String.class) args[i] = "가짜 상품";
                    else args[i] = null; // Enum 또는 기타 의존성
                }
                response = (ProductDetailResponse) targetConstructor.newInstance(args);
            }
        } catch (Exception e) {
            // 위 기법이 실패할 때를 대비한 최후의 방어선 백업
            response = org.mockito.Mockito.mock(ProductDetailResponse.class);
        }

        when(productService.getDetailProduct(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/products/1"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(productService, times(1)).getDetailProduct(1L);
    }

    @Test
    @DisplayName("상품 없음")
    void getProduct_notFound() throws Exception {
        // given
        when(productService.getDetailProduct(999L))
                .thenThrow(new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/products/999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}