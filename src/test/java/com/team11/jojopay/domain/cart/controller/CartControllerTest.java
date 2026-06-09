package com.team11.jojopay.domain.cart.controller;


import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.dto.request.UpdateCartItemQuantityRequest;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
import com.team11.jojopay.domain.cart.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;


    // 공통 Authentication 생성
    private RequestPostProcessor authentication() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        Collections.emptyList()
                );

        return request -> {
            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            return request;
        };
    }

    @Test
    @DisplayName("상품 담기 API 성공")
    void addCartItem() throws Exception {

        String requestBody = """
            {
              "productId": 1,
              "quantity": 2
            }
            """;

        mockMvc.perform(
                        post("/api/V1/cart/items")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(authentication())
                )
                .andExpect(status().isOk());

        verify(cartService)
                .addCartItem(
                        eq(1L),
                        any(AddCartItemRequest.class)
                );
    }

    @Test
    @DisplayName("장바구니 조회 API 성공")
    void getCart() throws Exception {

        CartResponse response =
                mock(CartResponse.class);

        when(cartService.getCart(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/V1/cart")
                                .with(authentication())
                )
                .andExpect(status().isOk());

        verify(cartService)
                .getCart(1L);
    }

    @Test
    @DisplayName("수량 변경 API 성공")
    void updateQuantity() throws Exception {

        String requestBody = """
            {
              "quantity": 5
            }
            """;

        mockMvc.perform(
                        put("/api/V1/cart/items/1")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(authentication())
                )
                .andExpect(status().isOk());

        verify(cartService)
                .updateQuantity(
                        eq(1L),
                        eq(1L),
                        any(UpdateCartItemQuantityRequest.class)
                );
    }

    @Test
    @DisplayName("장바구니 상품 삭제 API 성공")
    void deleteCartItem() throws Exception {

        mockMvc.perform(
                        delete("/api/V1/cart/items/1")
                                .with(authentication())
                )
                .andExpect(status().isOk());

        verify(cartService)
                .deleteCartItem(
                        1L,
                        1L
                );
    }

    @Test
    @DisplayName("장바구니 전체 비우기 API 성공")
    void clearCart() throws Exception {

        mockMvc.perform(
                        delete("/api/V1/cart")
                                .with(authentication())
                )
                .andExpect(status().isOk());

        verify(cartService)
                .clearCart(1L);
    }

    @Test
    @DisplayName("상품 ID 없으면 400")
    void addCartItem_invalidProductId() throws Exception {

        String requestBody = """
            {
              "quantity": 2
            }
            """;

        mockMvc.perform(
                        post("/api/V1/cart/items")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(authentication())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수량은 1 이상이여야 합니다.")
    void addCartItem_invalidQuantity() throws Exception {

        String requestBody = """
            {
              "productId": 1,
              "quantity": 0
            }
            """;

        mockMvc.perform(
                        post("/api/V1/cart/items")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                                .with(authentication())
                )
                .andExpect(status().isBadRequest());
    }

}
