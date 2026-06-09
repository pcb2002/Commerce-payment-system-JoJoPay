package com.team11.jojopay.domain.webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @InjectMocks
    private WebhookService webhookService;

    @Test
    @DisplayName("웹훅 서비스 검증: PAYMENT_SUCCESS 이벤트를 상신하면 예외 없이 성공 핸들러 분기를 관통한다.")
    void processPaymentEvent_Success_PaymentSuccess() {
        // given: 결제 성공 웹훅 요청 데이터 조립
        WebhookRequest request = new WebhookRequest("PAYMENT_SUCCESS", 100L, 50000L, "DONE");

        // when & then: 내부 handlePaymentSuccess 메서드가 안정적으로 가동되는지 검증
        assertDoesNotThrow(() -> webhookService.processPaymentEvent(request));
    }

    @Test
    @DisplayName("웹훅 서비스 검증: PAYMENT_CANCEL 이벤트를 상신하면 예외 없이 취소 핸들러 분기를 관통한다.")
    void processPaymentEvent_Success_PaymentCancel() {
        // given: 결제 취소 웹훅 요청 데이터 조립
        WebhookRequest request = new WebhookRequest("PAYMENT_CANCEL", 100L, 50000L, "CANCELED");

        // when & then: 내부 handlePaymentCancel 메서드가 안정적으로 가동되는지 검증
        assertDoesNotThrow(() -> webhookService.processPaymentEvent(request));
    }

    @Test
    @DisplayName("웹훅 서비스 검증: 정의되지 않은 유령 이벤트 타입이 들어와도 시스템이 크래시되지 않고 로그 경고 분기로 안전하게 우회한다.")
    void processPaymentEvent_Success_UnknownType() {
        // given: 지원하지 않는 가짜 이벤트 타입 매핑
        WebhookRequest request = new WebhookRequest("UNKNOWN_EVENT_TYPE", 999L, 0L, "UNKNOWN");

        // when & then: else 분기문을 정상적으로 밟고 지나가는지 검증
        assertDoesNotThrow(() -> webhookService.processPaymentEvent(request));
    }
}
