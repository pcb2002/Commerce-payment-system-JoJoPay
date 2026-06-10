package com.team11.jojopay.domain.webhook.controller;

import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 외부 PG사로부터 결제 변동 이벤트 웹훅을 수신합니다.
     */
    @PostMapping("/payment")
    public CommonApiResponse<Void> handlePaymentWebhook(
            @Valid @RequestBody WebhookRequest request
    ) {
        // 비즈니스 서비스 로직 호출
        webhookService.processPaymentEvent(request);

        // PG사 수신 성공 응답 명세 반환 (200 OK)
        return CommonApiResponse.success(OK, "웹훅 수신 및 처리 성공", null);
    }
}
