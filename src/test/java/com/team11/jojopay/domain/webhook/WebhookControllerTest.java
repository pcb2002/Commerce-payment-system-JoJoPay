package com.team11.jojopay.domain.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.webhook.controller.WebhookController;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(WebhookController.class)
@AutoConfigureMockMvc(addFilters = false) // 외부 통지 수신 API 특성상 시큐리티 인증 필터를 무력화
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebhookService webhookService;

    @MockitoBean
    private JwtProvider jwtProvider;
    // 💡 참고: 만약 버전에 따라 @MockitoBean 에서 컴파일 에러가 난다면 구버전 규격인 `@MockBean`으로 바꿔주시면 됩니다.

    @Test
    @DisplayName("웹훅 API 성공 검증: 올바른 포트원 양식의 페이로드가 인입되면 200 OK와 공통 메시지가 반환된다.")
    void handleWebhook_Api_Success() throws Exception {
        // given
        WebhookRequest request = new WebhookRequest("PAYMENT_SUCCESS", "imp_v2_99418247");
        doNothing().when(webhookService).processPaymentEvent(any(WebhookRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("웹훅 처리 완료"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(webhookService, times(1)).processPaymentEvent(any(WebhookRequest.class));
    }

    @Test
    @DisplayName("웹훅 API 유효성 실패 검증: 필수값인 포트원 거래키가 누락되어 인입되면 400 Bad Request 로 필터링 차단된다.")
    void handleWebhook_Api_ValidationFailed() throws Exception {
        // given
        WebhookRequest invalidRequest = new WebhookRequest("PAYMENT_SUCCESS", "");

        // when & then
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}