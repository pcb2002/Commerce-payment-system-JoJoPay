package com.team11.jojopay.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.ObjectMapper;
import com.team11.jojopay.domain.payment.dto.request.PaymentConfirmRequest;
import com.team11.jojopay.domain.payment.dto.response.PaymentResponse;
import com.team11.jojopay.domain.payment.service.PaymentService;
import com.team11.jojopay.common.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
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

  @MockitoBean
  private JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @Test
  @WithMockUser
  @DisplayName("결제 확정 API 성공: 실제 응답 데이터를 빌더로 생성하여 JSON 직렬화 및 결과 스펙을 검증한다.")
  void confirmPayment_Api_Success() throws Exception {
    // given: 요청 객체 준비
    PaymentConfirmRequest request = new PaymentConfirmRequest("ORD-2026-001", "imp_123456");

    // 에러 메시지에 나온 타입 순서대로 (Long, String, Long, LocalDateTime) 명시해야 합니다.
    java.lang.reflect.Constructor<PaymentResponse> constructor =
        PaymentResponse.class.getDeclaredConstructor(
            Long.class, String.class, Long.class, java.time.LocalDateTime.class
        );

    // private 접근 제어를 잠시 해제(무력화)합니다.
    constructor.setAccessible(true);

    // 인스턴스를 강제로 생성합니다. (타입 주의: 1L, 문자열, 150000L, 현재시간)
    PaymentResponse realResponse = constructor.newInstance(
        1L, "ORD-2026-001", 150000L, java.time.LocalDateTime.now()
    );

    // 서비스가 진짜 데이터가 든 realResponse를 반환하도록 모킹
    given(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).willReturn(realResponse);

    // when & then
    mockMvc.perform(post("/api/v1/payments/confirm")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").exists());

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
        .andDo(print())
        .andExpect(status().isBadRequest());

    verify(paymentService, never()).confirmPayment(any());
  }
}
