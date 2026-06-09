package com.team11.jojopay.domain.webhook.service;

import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    // private final PointService pointService; // 필요 시 포인트 서비스 연동

    /**
     * 웹훅 결제 이벤트를 해시 분석하여 도메인 원장에 반영합니다.
     */
    @Transactional
    public void processPaymentEvent(WebhookRequest request) {
        log.info("▶ [Webhook 수신] 이벤트유형: {}, 결제ID: {}, 금액: {}",
                request.getEventType(), request.getPaymentId(), request.getAmount());

        // TODO: [중복 수신 방지 가드레일]
        // 외부 웹훅은 동일 이벤트가 2번 이상 인입될 수 있으므로,
        // 이미 처리된 paymentId 인지 테이블을 먼저 조회(또는 분산 락/레디스 검증)하는 로직이 권장됩니다.

        // 이벤트 분기 처리 예시
        if ("PAYMENT_SUCCESS".equals(request.getEventType())) {
            handlePaymentSuccess(request);
        } else if ("PAYMENT_CANCEL".equals(request.getEventType())) {
            handlePaymentCancel(request);
        } else {
            log.warn("미지원 웹훅 이벤트 타입 인입: {}", request.getEventType());
        }
    }

    private void handlePaymentSuccess(WebhookRequest request) {
        log.info("✔ 결제 완료 원장 승인 처리 진행 - 결제ID: {}", request.getPaymentId());
        // 예: pointService.createHistory(memberId, payment, PointTransactionType.EARN, request.getAmount());
    }

    private void handlePaymentCancel(WebhookRequest request) {
        log.info("✈ 결제 취소 원장 환불 처리 진행 - 결제ID: {}", request.getPaymentId());
        // 예: pointService.createHistory(memberId, payment, PointTransactionType.EARN_FORFEIT, request.getAmount());
    }
}
