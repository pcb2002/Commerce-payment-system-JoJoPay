package com.team11.jojopay.domain.product.controller;


import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;


    @Test
    @DisplayName("상품 목록 조회 성공")
    void getProducts_success() throws Exception {

        // given
        Page<ProductListResponse> page =
                new PageImpl<>(List.of());

        when(productService.getProductList(any(ProductSearchRequest.class)))
                .thenReturn(page);

        // when & then
        mockMvc.perform(
                        get("/api/v1/products")
                )
                .andExpect(status().isOk());

        verify(productService)
                .getProductList(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 목록 조회 필터 조건 전달")
    void getProducts_withCondition() throws Exception {

        Page<ProductListResponse> page =
                new PageImpl<>(List.of());

        when(productService.getProductList(any()))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("category", "ELECTRONICS")
                                .param("minPrice", "1000")
                                .param("maxPrice", "5000")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk());

        verify(productService)
                .getProductList(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getProduct_success() throws Exception {

        ProductDetailResponse response =
                mock(ProductDetailResponse.class);

        when(productService.getDetailProduct(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/products/1")
                )
                .andExpect(status().isOk());

        verify(productService)
                .getDetailProduct(1L);
    }

    @Test
    @DisplayName("상품 없음")
    void getProduct_notFound() throws Exception {

        when(productService.getDetailProduct(999L))
                .thenThrow(
                        new ServiceException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );

        mockMvc.perform(
                        get("/api/v1/products/999")
                )
                .andExpect(status().isNotFound());
    }

}
