package com.team11.jojopay.domain.cart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.cart.controller.CartController;
import com.team11.jojopay.domain.cart.dto.request.AddCartItemRequest;
import com.team11.jojopay.domain.cart.dto.request.UpdateCartItemQuantityRequest;
import com.team11.jojopay.domain.cart.dto.response.CartResponse;
import com.team11.jojopay.domain.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JpaMetamodelMappingContext.class)
public class CartControllerTest {

    private MockMvc mockMvc; // 수동 설정을 위해 final 제거 및 주입 방식 변경

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CartController cartController; // 실제 타겟 컨트롤러 주입받기

    @BeforeEach
    void setUp() {
        // MockMvc가 구동될 때 @AuthenticationPrincipal 어노테이션을 만나면
        // 시큐리티 세션을 뒤지지 않고 무조건 1L을 꽂아주도록 테스트 전용 리졸버(치트키)를 빌더에 강제 이식합니다.
        this.mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return 1L; // 여기에 적힌 1L이 컨트롤러 memberId 파라미터로 다이렉트 자동 강제 주입
                    }
                })
                .build();
    }

    @Test
    @DisplayName("상품 담기 API 성공")
    void addCartItem() throws Exception {
        // given
        String requestBody = "{\"productId\":1,\"quantity\":2}";

        // when & then
        mockMvc.perform(post("/api/V1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());

        verify(cartService).addCartItem(eq(1L), any(AddCartItemRequest.class));
    }

    @Test
    @DisplayName("장바구니 조회 API 성공")
    void getCart() throws Exception {
        // given
        CartResponse response = mock(CartResponse.class);
        when(cartService.getCart(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/V1/cart"))
                .andExpect(status().isOk());

        verify(cartService).getCart(eq(1L));
    }

    @Test
    @DisplayName("수량 변경 API 성공")
    void updateQuantity() throws Exception {
        // given
        String requestBody = "{\"quantity\":5}";

        // when & then
        mockMvc.perform(put("/api/V1/cart/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(cartService).updateQuantity(eq(1L), eq(1L), any(UpdateCartItemQuantityRequest.class));
    }

    @Test
    @DisplayName("장바구니 상품 삭제 API 성공")
    void deleteCartItem() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/V1/cart/items/1"))
                .andExpect(status().isOk());

        verify(cartService).deleteCartItem(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("장바구니 전체 비우기 API 성공")
    void clearCart() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/V1/cart"))
                .andExpect(status().isOk());

        verify(cartService).clearCart(eq(1L));
    }

    @Test
    @DisplayName("상품 ID 없으면 400")
    void addCartItem_invalidProductId() throws Exception {
        // given
        String requestBody = "{\"quantity\":2}";

        // when & then
        mockMvc.perform(post("/api/V1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수량은 1 이상이여야 합니다.")
    void addCartItem_invalidQuantity() throws Exception {
        // given
        String requestBody = "{\"productId\":1,\"quantity\":0}";

        // when & then
        mockMvc.perform(post("/api/V1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}