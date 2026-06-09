package com.team11.jojopay.domain.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.subscription.controller.SubscriptionController;
import com.team11.jojopay.domain.subscription.dto.request.SubscriptionStartRequest;
import com.team11.jojopay.domain.subscription.dto.response.SubscriptionResponse;
import com.team11.jojopay.domain.subscription.enums.SubscriptionStatus;
import com.team11.jojopay.domain.subscription.service.SubscriptionService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false) // 🟢 시큐리티 상시 차단선
@MockitoBean(types = JpaMetamodelMappingContext.class)
class SubscriptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private SubscriptionService subscriptionService;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  @WithMockUser
  @DisplayName("[POST] 정기 구독 신청 API 가동 성공")
  void startSubscription_Success() throws Exception {
    // given
    SubscriptionStartRequest request = objectMapper.readValue(
        "{\"billingKeyId\":1,\"plan\":\"PREMIUM\"}",
        SubscriptionStartRequest.class
    );

    // 내부 DTO 컴파일 스펙 동기화
    java.lang.reflect.Constructor<SubscriptionResponse> constructor =
        SubscriptionResponse.class.getDeclaredConstructor(
            Long.class, String.class, Long.class, SubscriptionStatus.class, LocalDate.class
        );
    constructor.setAccessible(true);
    SubscriptionResponse mockResponse = constructor.newInstance(55L, "프리미엄 정기 요금제", 29900L, SubscriptionStatus.ACTIVE, LocalDate.now());

    given(subscriptionService.startSubscription(any(), any(SubscriptionStartRequest.class))).willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("구독 시작 완료"))
        .andExpect(jsonPath("$.data.subscriptionId").value(55));
  }

  @Test
  @WithMockUser
  @DisplayName("[GET] 내 구독 정보 단건 조회 성공")
  void getMySubscription_Success() throws Exception {
    // given
    given(subscriptionService.getMySubscription(any())).willReturn(null);

    // when & then
    mockMvc.perform(get("/api/v1/subscriptions/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("내 구독 조회 완료"));

    verify(subscriptionService, times(1)).getMySubscription(any());
  }

  @Test
  @WithMockUser
  // 🔴 [복구 완료 지점]: 직전 턴에 잘려 나갔던 명시적 해지 연동 검증부 완치
  @DisplayName("[POST] 내 구독 취소 해지 성공")
  void cancelSubscription_Success() throws Exception {
    // given
    given(subscriptionService.cancelSubscription(any())).willReturn(null);

    // when & then
    mockMvc.perform(post("/api/v1/subscriptions/me/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("구독 해지 완료"));

    verify(subscriptionService, times(1)).cancelSubscription(any());
  }

  @Test
  @WithMockUser
  @DisplayName("[GET] 내 정기 결제 청구서 이력 전체 조회 성공")
  void getMySubscriptionBillings_Success() throws Exception {
    // given
    given(subscriptionService.getMySubscriptionBillings(any())).willReturn(List.of());

    // when & then
    mockMvc.perform(get("/api/v1/subscriptions/me/billings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("내 구독 결제 내역 조회 완료"))
        .andExpect(jsonPath("$.data").isArray());

    verify(subscriptionService, times(1)).getMySubscriptionBillings(any());
  }
}
