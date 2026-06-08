package com.team11.jojopay.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.service.PaymentService;
import com.team11.jojopay.common.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentController.class)
public class PaymentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private PaymentService paymentService;

  @MockitoBean
  private JwtProvider jwtProvider;

  // [메인 클래스 수정 방지 장치]
  // 메인 클래스에 @EnableJpaAuditing이 붙어있어 발생하는 엔티티 미로드 오류를 이 가짜 빈 한 줄로 완벽히 무력화합니다.
  @MockitoBean
  private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @Test
  @WithMockUser // 가짜 로그인 유저 신분증 장착 (403 방어)
  @DisplayName("결제 확정 API 성공: 올바른 JSON 요청 시 200 OK를 반환하고 서비스 층을 호출한다.")
  void confirmPayment_Api_Success() throws Exception {
    // given
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-2026-001", "imp_123456");
    PaymentResponse mockResponse = mock(PaymentResponse.class);

    given(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/payments/confirm")
            .with(csrf()) // 가짜 CSRF 인증 통과 패스포트 (403 방어)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(paymentService, times(1)).confirmPayment(any(PaymentConfirmRequest.class));
  }

  @Test
  @WithMockUser
  @DisplayName("결제 확정 API 실패: 필수 파라미터인 결제 고유 ID가 누락되면 400 Bad Request를 반환한다.")
  void confirmPayment_Api_Fail_Validation() throws Exception {
    // given: @NotBlank를 위반하도록 포트원 ID를 공백("")으로 설정
    PaymentConfirmRequest invalidRequest = new PaymentConfirmRequest("ORD-2026-001", "");

    // when & then
    mockMvc.perform(post("/api/v1/payments/confirm")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest()); // 시큐리티를 넘어 정상적으로 컨트롤러 @Valid 유효성 검증 단에서 컷(400)됩니다.

    verify(paymentService, never()).confirmPayment(any());
  }
}