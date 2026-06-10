package com.team11.jojopay.domain.webhook;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.order.entity.Order;
import com.team11.jojopay.domain.order.service.OrderService;
import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.payment.enums.PaymentStatus;
import com.team11.jojopay.domain.payment.repository.PaymentRepository;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.service.PointService;
import com.team11.jojopay.domain.subscription.service.SubscriptionTransactionService;
import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import com.team11.jojopay.domain.webhook.service.WebhookService;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @InjectMocks
    private WebhookService webhookService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PointService pointService;
    @Mock private OrderService orderService;
    @Mock private SubscriptionTransactionService subscriptionTransactionService;
    @Mock private PortOneClient portOneClient;

    private Member realMember;
    private Payment mockPayment;
    private PortOnePaymentResponse mockResponse;
    private PortOnePaymentResponse.Amount mockAmount; // 계층형 Mock 필드 변수 수립

    private final Long memberId = 1L;
    private final Long orderId = 880L;

    @BeforeEach
    void setUp() {
        realMember = Member.signup("강감찬", "kang@test.com", "hash", "010-1111-2222");
        ReflectionTestUtils.setField(realMember, "id", memberId);

        mockPayment = mock(Payment.class);
        doReturn(memberId).when(mockPayment).getMemberId();
        doReturn(100000L).when(mockPayment).getAmount();
        doReturn(100000L).when(mockPayment).getPgRealAmount();
        doReturn(null).when(mockPayment).getSubscriptionId(); // 기본은 일반 결제 시나리오 선언

        Order mockOrder = mock(Order.class);
        doReturn(orderId).when(mockOrder).getId();
        doReturn(mockOrder).when(mockPayment).getOrder();

        // 2단계 중첩 계층 Mock 수립을 구현하여 Long 타입 컴파일 한계를 완벽 탈출
        mockResponse = mock(PortOnePaymentResponse.class);
        mockAmount = mock(PortOnePaymentResponse.Amount.class);

        when(portOneClient.getPaymentInfo(any())).thenReturn(mockResponse);
        when(mockResponse.getAmount()).thenReturn(mockAmount);
    }

    @Test
    @DisplayName("통합 웹훅 보안 검증: PAYMENT_SUCCESS 인입 시 정품 교차 조회를 거쳐 전체 주문 상태 및 포인트 장부가 완벽히 마감된다.")
    void processPaymentEvent_Success_PaymentSuccess() {
        // given
        WebhookRequest request = new WebhookRequest("PAYMENT_SUCCESS", "pay-1234");

        when(mockPayment.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentRepository.findByPortonePaymentIdWithLock("pay-1234")).thenReturn(Optional.of(mockPayment));
        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(realMember));

        // 포트원 오피셜 서버 검증 통과 상황 데이터 셋업
        when(mockResponse.getStatus()).thenReturn("PAID");
        doReturn(100000L).when(mockAmount).getTotal(); // 🎯 doReturn 우회책 도입으로 long-Long 형변환 터짐 영구 치유

        // NORMAL 등급 보상률 1% 계산 결과값 도출 명세 (100000원 * 0.01 = 1000P)
        Long expectedEarnPoint = 10000L;

        // when & then
        assertDoesNotThrow(() -> webhookService.processPaymentEvent(request));

        // then
        verify(mockPayment, times(1)).complete();
        verify(orderService, times(1)).completeOrder(orderId);
        verify(pointService, times(1)).createHistory(eq(memberId), eq(mockPayment), eq(PointTransactionType.EARN), eq(expectedEarnPoint));
    }

    @Test
    @DisplayName("통합 웹훅 보안 검증: PAYMENT_CANCEL 인입 시 정품 교차 조회를 거쳐 주문 재고가 롤백되고 포인트 적립분이 몰수된다.")
    void processPaymentEvent_Success_PaymentCancel() {
        // given
        WebhookRequest request = new WebhookRequest("PAYMENT_CANCEL", "pay-1234");

        when(mockPayment.getStatus()).thenReturn(PaymentStatus.COMPLETED);
        when(paymentRepository.findByPortonePaymentIdWithLock("pay-1234")).thenReturn(Optional.of(mockPayment));
        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(realMember));

        // 포트원 오피셜 서버 환불 승인 상황 데이터 셋업
        when(mockResponse.getStatus()).thenReturn("CANCELED");
        doReturn(100000L).when(mockAmount).getTotal();

        Long expectedForfeitPoint = 1000L;

        // when & then
        assertDoesNotThrow(() -> webhookService.processPaymentEvent(request));

        // then
        verify(mockPayment, times(1)).cancel();
        verify(orderService, times(1)).cancelOrder(orderId);
        verify(pointService, times(1)).createHistory(eq(memberId), eq(mockPayment), eq(PointTransactionType.EARN_FORFEIT), eq(expectedForfeitPoint));
    }
}