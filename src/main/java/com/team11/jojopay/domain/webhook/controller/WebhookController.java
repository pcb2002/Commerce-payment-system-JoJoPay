package com.team11.jojopay.domain.webhook.controller;

import static org.springframework.http.HttpStatus.OK;

import com.team11.jojopay.common.response.CommonApiResponse;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 포트원 사후 정산 서버로부터 비동기 결제 변동 이벤트를 수신하여 통합 도메인 장부 마감을 실행합니다.
     */
    @PostMapping("/webhook")
    public CommonApiResponse<Void> handleWebhook(@Valid @RequestBody WebhookRequest request) {
        log.info("[Webhook Controller 인입] 이벤트: {}, 포트원 고유ID: {}",
                request.getEventType(), request.getPortonePaymentId());

        // 의존성 주입된 웹훅 서비스로 도메인 파이프라인 제어권 위임
        webhookService.processPaymentEvent(request);

        // 팀 전역 공통 응답 포맷 상자에 패키징하여 200 OK 반환
        return CommonApiResponse.success(HttpStatus.OK, "웹훅 처리 완료", null);
    }
}