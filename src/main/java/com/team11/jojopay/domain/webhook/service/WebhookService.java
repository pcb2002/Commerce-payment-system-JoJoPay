package com.team11.jojopay.domain.webhook.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.service.OrderService;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
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
    private final PointService pointService; // 🔗 포인트 서비스 연동
    private final OrderService orderService; // 🔗 확장 구현한 주문 서비스 연동

    /**
     * 외부 PG사 웹훅 이벤트를 수신하여 전체 장부(결제, 주문, 회원멤버십, 포인트지갑)를 동기화 마감합니다.
     */
    @Transactional
    public void processPaymentEvent(WebhookRequest request) {
        log.info("▶ [Webhook 통합 마감] 타입: {}, 결제ID: {}", request.getEventType(), request.getPaymentId());

        // 1. 원본 결제 장부 조회
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 중복 수신 방지 가드레일 (Idempotency Guard)
        if ("PAYMENT_SUCCESS".equals(request.getEventType()) && "DONE".equals(payment.getStatus())) {
            log.warn("이미 성공적으로 마감 완료된 결제 건입니다. 결제ID: {}", request.getPaymentId());
            return;
        }
        if ("PAYMENT_CANCEL".equals(request.getEventType()) && "CANCELED".equals(payment.getStatus())) {
            log.warn("이미 환불/취소 마감 완료된 결제 건입니다. 결제ID: {}", request.getPaymentId());
            return;
        }

        // 3. 트랜잭션 도미노 연쇄 분기 처리
        if ("PAYMENT_SUCCESS".equals(request.getEventType())) {
            handlePaymentSuccess(payment, request);
        } else if ("PAYMENT_CANCEL".equals(request.getEventType())) {
            handlePaymentCancel(payment, request);
        } else {
            log.warn("지원하지 않는 웹훅 이벤트 타입 인입: {}", request.getEventType());
        }
    }

    /**
     * 결제 성공 마감 흐름
     */
    private void handlePaymentSuccess(Payment payment, WebhookRequest request) {
        // [Step A] 객체지향적 도메인 비즈니스 메서드 호출 (완료 상태 전이)
        payment.complete();

        // [Step B] 주문 도메인 연동 -> 주문 완료 및 배송 준비 상태 전환 (재고 최종 차감 확정)
        if (payment.getOrder() != null) {
            orderService.completeOrder(payment.getOrder().getId());
        }

        // [Step C] 회원 도메인 연동 -> 누적 결제금액 증액 및 멤버십 등급 실시간 자동 갱신
        Long memberId = payment.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));
        member.increaseTotalPaymentAmount(request.getAmount());

        // [Step D] 포인트 도메인 연동 -> 등급별 적립 원장 적재
        pointService.createHistory(memberId, payment, PointTransactionType.EARN, request.getAmount());

        log.info("✔ [결제 성공 동기화 완료] 회원ID: {}, 현재등급: {}", memberId, member.getMembershipGrade());
    }

    /**
     * 결제 취소(환불) 롤백 흐름
     */
    private void handlePaymentCancel(Payment payment, WebhookRequest request) {
        // [Step A] 기존 OrderService 정책과 일치하는 도메인 비즈니스 메서드 호출 (취소 상태 전이)
        payment.cancel();

        // [Step B] 주문 도메인 연동 -> 주문 취소 처리 및 차감되었던 상품 재고 오름차순 비관적 락 복구
        if (payment.getOrder() != null) {
            orderService.cancelOrder(payment.getOrder().getId());
        }

        // [Step C] 회원 도메인 연동 -> 환불액만큼 누적 결제금액 차감 및 등급 강등 재계산
        Long memberId = payment.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBER_NOT_FOUND));
        member.decreaseTotalPaymentAmount(request.getAmount());

        // [Step D] 포인트 도메인 연동 -> 기존 적립분 회수 및 몰수 이력 원장 적재
        pointService.createHistory(memberId, payment, PointTransactionType.EARN_FORFEIT, request.getAmount());

        log.info("✔ [결제 환불 동기화 완료] 회원ID: {}, 현재등급: {}", memberId, member.getMembershipGrade());
    }
}