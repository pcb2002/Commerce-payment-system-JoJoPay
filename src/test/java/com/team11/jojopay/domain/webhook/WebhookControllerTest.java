package com.team11.jojopay.domain.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.webhook.controller.WebhookController;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
@WebMvcTest(WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockitoBean(types = JpaMetamodelMappingContext.class)
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebhookService webhookService;

    @MockitoBean
    private JwtProvider jwtProvider; // 시큐리티 가드 컴파일 방어용 모킹

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("[POST] 외부 PG사 결제 완료 웹훅 수신 성공: JSON 페이로드가 정상 직렬화되어 서비스 로직을 트리거한다.")
    void handlePaymentWebhook_Success() throws Exception {
        // given: 외부 PG사가 쏴주는 정상 웹훅 포맷 데이터 조립
        WebhookRequest webhookRequest = new WebhookRequest("PAYMENT_SUCCESS", 550L, 25000L, "DONE");
        String requestBody = objectMapper.writeValueAsString(webhookRequest);

        // when & then: 인증 토큰 없이 외부에서 다이렉트로 들어오는 인바운드 POST 호출 시뮬레이션
        mockMvc.perform(post("/api/v1/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk()); // 수신 완료 처리 응답 (200 OK)

        // then: 웹훅 서비스 단으로 DTO 데이터 조립 알맹이가 유실 없이 도달했는지 추적 단언
        verify(webhookService, times(1)).processPaymentEvent(any(WebhookRequest.class));
    }

    @Test
    @DisplayName("[POST] 웹훅 수신 실패: 필수 본문 파라미터가 누락되거나 바인딩이 깨지면 400 Bad Request가 발생한다.")
    void handlePaymentWebhook_Fail_InvalidPayload() throws Exception {
        // given: 구조가 완전히 깨진 부적절한 형태의 가짜 JSON 데이터 상신
        String invalidJson = "{ \"eventType\": \"PAYMENT_SUCCESS\", \"paymentId\": \"이 자리에 숫자가 와야 하는데 문자열이 옴\" }";

        // when & then
        mockMvc.perform(post("/api/v1/webhooks/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 스프링 서블릿 단에서 파싱 크래시 방어 (400)
    }
}