package com.team11.jojopay.domain.webhook.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.service.OrderService;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.service.SubscriptionTransactionService;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final OrderService orderService;
    private final SubscriptionTransactionService subscriptionTransactionService;
    private final PortOneClient portOneClient;

    /**
     * 외부 포트원 웹훅 통지를 기반으로 교차 검증을 거쳐 전체 원장(결제/주문/구독/멤버십/포인트)을 최종 마감합니다.
     */
    @Transactional
    public void processPaymentEvent(WebhookRequest request) {
        log.info("▶ [Webhook 정밀 검증 가동] 포트원 ID: {}", request.getPortonePaymentId());

        // 1. [보안 교차 검증] 본문 데이터를 신뢰하지 않고 포트원 오피셜 API 서버 데이터 강제 직접 조회
        PortOnePaymentResponse verifiedData = portOneClient.getPaymentInfo(request.getPortonePaymentId());

        // 2. [비관적 쓰기 락 선점] DB 레이어에서 결제 원장 선점 (동시성 중복 처리 완전 차단)
        Payment payment = paymentRepository.findByPortonePaymentIdWithLock(request.getPortonePaymentId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 3. 비관적 락 컨텍스트 내부 안전지대에서 멱등성(중복 수신) 가드레일 가동
        if ("PAYMENT_SUCCESS".equals(request.getEventType()) && payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("이미 성공 마감 완료된 결제 건입니다. 중복 패스.");
            return;
        }
        if ("PAYMENT_CANCEL".equals(request.getEventType()) && payment.getStatus() == PaymentStatus.CANCELED) {
            log.warn("이미 취소 마감 완료된 결제 건입니다. 중복 패스.");
            return;
        }

        // 4. 변조 불가능한 포트원 정품 응답 상세 내역 기반 최종 데이터 추출
        String portoneStatus = verifiedData.getStatus();
        Long realAmount = verifiedData.getAmount().getTotal(); // 계층형 대완치 교정 완료

        // 5. [정기 구독 도메인 예외 분기 통합] 일반 주문 결제가 아닌 정기 구독 자동 결제 갱신 갱신 건일 경우 제어권 이관
        if (payment.getSubscriptionId() != null) {
            handleSubscriptionWebhook(payment, request.getEventType(), portoneStatus);
            return;
        }

        // 6. 일반 커머스 상품 주문 결제 파이프라인 구동
        if ("PAYMENT_SUCCESS".equals(request.getEventType()) && "PAID".equals(portoneStatus)) {
            handleOrderPaymentSuccess(payment, realAmount);
        } else if ("PAYMENT_CANCEL".equals(request.getEventType()) && "CANCELED".equals(portoneStatus)) {
            handleOrderPaymentCancel(payment, realAmount);
        }
    }

    /**
     * 정기 구독 도메인 전용 결제 성공/실패 밸런싱 동기화 처리
     */
    private void handleSubscriptionWebhook(Payment payment, String eventType, String portoneStatus) {
        if ("PAYMENT_SUCCESS".equals(eventType) && "PAID".equals(portoneStatus)) {
            payment.complete();
            // 기존 정기결제 트랜잭션 서비스를 호출하여 청구이력 가산, 멤버십 승격, 구독연장 주기 일괄 마감
            subscriptionTransactionService.saveRenewSubscriptionSuccess(
                    payment.getSubscriptionId(), 1, "Webhook Auto Renewal", payment.getPortonePaymentId()
            );
            log.info("✔ [정기 구독 웹훅 보정 마감 완효] 구독 ID: {}", payment.getSubscriptionId());
        } else if ("PAYMENT_CANCEL".equals(eventType)) {
            payment.cancel();
            subscriptionTransactionService.saveRenewSubscriptionFailure(
                    payment.getSubscriptionId(), 1, "Webhook Cancel Notification"
            );
            log.info("✔ [정기 구독 취소 웹훅 보정 마감 완효] 구독 ID: {}", payment.getSubscriptionId());
        }
    }

    /**
     * [일반 주문 결제 성공]: 장부 정합성 마감 처리
     */
    private void handleOrderPaymentSuccess(Payment payment, Long realAmount) {
        payment.complete(); // 결제 엔티티 COMPLETED 전이 및 승인일시 스냅샷 마킹

        if (payment.getOrder() != null) {
            orderService.completeOrder(payment.getOrder().getId()); // 주문 엔티티 COMPLETED 확정 상태 전이
        }

        // 비관적 쓰기 락 기반 회원 지갑 확보
        Long memberId = payment.getMemberId();
        Member member = memberRepository.findByIdWithLock(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

        // [비즈니스 명세 정합성 일치] PaymentService 정책 그대로 포인트 결제액을 배제한 순수 PG 실긁힘 대금만 실적 가산
        member.increaseTotalPaymentAmount(payment.getPgRealAmount());

        // 등급별 보상 비율 단위 정산에 맞춘 구매 적립 포인트 가산 처리
        double earnRate = member.getMembershipGrade().getRewardRate() / 100.0;
        Long earnPoint = (long) (payment.getAmount() * earnRate);

        if (earnPoint > 0) {
            pointService.createHistory(member.getId(), payment, PointTransactionType.EARN, earnPoint);
        }

        log.info("✔ [일반주문 성공 웹훅 처리 완료] 회원ID: {}, 실적립: {}P", memberId, earnPoint);
    }

    /**
     * [일반 주문 결제 취소]: 재고 복구 및 자산 몰수 처리
     */
    private void handleOrderPaymentCancel(Payment payment, Long realAmount) {
        payment.cancel(); // 결제 엔티티 CANCELED 전이

        if (payment.getOrder() != null) {
            orderService.cancelOrder(payment.getOrder().getId()); // 주문 취소 및 상품 품목별 ID 오름차순 정렬 기반 비관적 락 재고 롤백 원상복구 가동
        }

        // 비관적 쓰기 락 회원 영수증 획득
        Long memberId = payment.getMemberId();
        Member member = memberRepository.findByIdWithLock(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));

        // 취소된 실결제액만큼 멤버십 누적 금액 감산 및 등급 실시간 실시간 다운그레이드 재계산
        member.decreaseTotalPaymentAmount(payment.getPgRealAmount());

        // 기지급 되었던 구매 적립금 무효화 환수 및 몰수 이력 원장 적재
        double earnRate = member.getMembershipGrade().getRewardRate() / 100.0;
        Long earnPoint = (long) (payment.getAmount() * earnRate);

        if (earnPoint > 0) {
            pointService.createHistory(member.getId(), payment, PointTransactionType.EARN_FORFEIT, earnPoint);
        }

        log.info("✔ [일반주문 취소 웹훅 처리 완료] 회원ID: {}, 회수액: {}P", memberId, earnPoint);
    }
}