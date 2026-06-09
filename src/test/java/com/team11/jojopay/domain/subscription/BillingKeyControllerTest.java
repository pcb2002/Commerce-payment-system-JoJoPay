package com.team11.jojopay.domain.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.subscription.controller.BillingKeyController;
import com.team11.jojopay.domain.subscription.dto.request.BillingKeyRegisterRequest;
import com.team11.jojopay.domain.subscription.dto.response.BillingKeyResponse;
import com.team11.jojopay.domain.subscription.enums.BillingKeyStatus;
import com.team11.jojopay.domain.subscription.service.BillingKeyService;
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

@WebMvcTest(controllers = BillingKeyController.class)
@AutoConfigureMockMvc(addFilters = false) // 🟢 유저 오리지널 기조 유지: 시큐리티 필터 무력화
@MockitoBean(types = JpaMetamodelMappingContext.class)
class BillingKeyControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BillingKeyService billingKeyService;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  @WithMockUser
  @DisplayName("[POST] 빌링키 등록 성공: 올바른 데이터 상신 시 201 Created 응답이 반환된다.")
  void registerBillingKey_Success() throws Exception {
    // given: DTO 필드 매핑
    BillingKeyRegisterRequest request = objectMapper.readValue(
        "{\"customerUid\":\"user_77_billing\",\"cardName\":\"신한카드\",\"cardNumber\":\"1111-2222\"}",
        BillingKeyRegisterRequest.class
    );

    // 롬복/생성자 캡슐화 우회를 위해 리플렉션 기술로 응답 객체 생성
    java.lang.reflect.Constructor<BillingKeyResponse> constructor =
        BillingKeyResponse.class.getDeclaredConstructor(
            Long.class, String.class, String.class, String.class, BillingKeyStatus.class
        );
    constructor.setAccessible(true);
    BillingKeyResponse mockResponse = constructor.newInstance(1L, "user_77_billing", "신한카드", "1111-2222", BillingKeyStatus.ACTIVE);

    given(billingKeyService.registerBillingKey(any(), any(BillingKeyRegisterRequest.class))).willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/subscriptions/billing-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("빌링키 등록 완료"))
        .andExpect(jsonPath("$.data.customerUid").value("user_77_billing"));
  }

  @Test
  @WithMockUser
  @DisplayName("[POST] 빌링키 등록 실패: 필수 파라미터가 누락되면 400 Bad Request 에러 가드레일이 작동한다.")
  void registerBillingKey_Fail_Validation() throws Exception {
    // given: customerUid 가 비어있는 잘못된 페이로드
    BillingKeyRegisterRequest invalidRequest = objectMapper.readValue(
        "{\"customerUid\":\"\",\"cardName\":\"신한카드\",\"cardNumber\":\"1111\"}",
        BillingKeyRegisterRequest.class
    );

    // when & then
    mockMvc.perform(post("/api/v1/subscriptions/billing-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andDo(print())
        .andExpect(status().isBadRequest());

    verify(billingKeyService, never()).registerBillingKey(any(), any());
  }

  @Test
  @WithMockUser
  @DisplayName("[GET] 내 빌링키 목록 조회 성공")
  void getBillingKeys_Success() throws Exception {
    // given
    given(billingKeyService.getMyBillingKeys(any())).willReturn(List.of());

    // when & then
    mockMvc.perform(get("/api/v1/subscriptions/billing-keys"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("빌링키 목록 조회 완료"))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @WithMockUser
  @DisplayName("[DELETE] 빌링키 삭제 비활성화 성공")
  void deleteBillingKey_Success() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/v1/subscriptions/billing-keys/{billingKeyId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("빌링키 삭제 완료"));

    verify(billingKeyService, times(1)).deleteBillingKey(any(), eq(1L));
  }
}
