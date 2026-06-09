package com.team11.jojopay.domain.order;

// Jackson 3 표준 패키지 경로
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.order.controller.OrderController;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import com.team11.jojopay.domain.order.dto.request.OrderCreateRequest;
import com.team11.jojopay.domain.order.dto.request.OrderPreviewRequest;
import com.team11.jojopay.domain.order.dto.response.*;
import com.team11.jojopay.domain.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// 스프링 부트 4 새로운 모킹 오버라이드 패키지 경로
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
// import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)// 필터 무시
@MockitoBean(types = JpaMetamodelMappingContext.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    // 컨트롤러 테스트에서 필요 없도록 Mock으로만 띄웁니다.
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private com.team11.jojopay.common.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    @DisplayName("[POST] 주문서 미리보기 API 성공")
    void preview() throws Exception {
        // given: 실제 데이터처럼 구조를 갖춘 객체 생성
        String jsonPayload = """
                {
                    "cartItemIds": [1, 2]
                }
                """;

        // 빌더를 이용해 실제 데이터가 담긴 응답 객체 생성
        OrderPreviewResponse.PreviewItemResponse item = OrderPreviewResponse.PreviewItemResponse.builder()
                .productId(1L)
                .productName("키보드")
                .price(150000L)
                .quantity(1)
                .build();

        OrderPreviewResponse realResponse = OrderPreviewResponse.builder()
                .items(List.of(item))
                .totalAmount(150000L)
                .build();

        given(orderService.preview(any(), any(OrderPreviewRequest.class))).willReturn(realResponse);

        // when & then
        mockMvc.perform(post("/api/v1/orders/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주문서 미리보기 성공"))
                .andExpect(jsonPath("$.data.totalAmount").value(150000L))
                .andExpect(jsonPath("$.data.items[0].productName").value("키보드"));
    }

    @Test
    @WithMockUser
    @DisplayName("[POST] 주문 생성 API 성공")
    void createOrder() throws Exception {
        String jsonPayload = """
                {
                    "cartItemIds": [1, 2],
                    "usedPoint": 0
                }
                """;

        // 💡 mock() 대신 빌더로 실제 객체 생성
        OrderResponse realResponse = OrderResponse.builder()
                .orderNumber("ORD-1234")
                .totalAmount(10000L)
                .build();

        given(orderService.createOrder(any(), any(OrderCreateRequest.class))).willReturn(realResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주문 성공"))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-1234")); // 실제 데이터 확인
    }

    @Test
    @WithMockUser
    @DisplayName("[GET] 내 주문 내역 목록 조회 API 성공")
    void getMyOrders() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);

        // 💡 실제 객체 리스트 생성
        OrderListItemResponse item = OrderListItemResponse.builder()
                .orderNumber("ORD-1234")
                .totalAmount(10000L)
                .build();

        Page<OrderListItemResponse> realPage = new PageImpl<>(List.of(item), pageable, 1);

        given(orderService.getMyOrders(any(), any(Pageable.class))).willReturn(realPage);

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-1234"));
    }

    @Test
    @WithMockUser
    @DisplayName("[GET] 단건 주문 상세 조회 API 성공")
    void getOrderDetail() throws Exception {
        String orderNumber = "ORD-1234";

        // 💡 실제 객체 생성
        OrderDetailResponse realResponse = OrderDetailResponse.builder()
                .orderNumber(orderNumber)
                .build();

        given(orderService.getOrderDetail(any(), eq(orderNumber))).willReturn(realResponse);

        mockMvc.perform(get("/api/v1/orders/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber));
    }

    @Test
    @WithMockUser
    @DisplayName("[POST] 주문 취소 API 성공")
    void cancelOrder() throws Exception {
        String orderNumber = "ORD-1234";

        // 💡 실제 객체 생성
        OrderCancelResponse realResponse = OrderCancelResponse.builder()
                .orderNumber(orderNumber)
                .build();

        given(orderService.cancelOrder(any(), eq(orderNumber))).willReturn(realResponse);

        mockMvc.perform(post("/api/v1/orders/{orderNumber}/cancel", orderNumber)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber));
    }
}