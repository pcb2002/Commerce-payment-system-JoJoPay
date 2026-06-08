package com.team11.jojopay.domain.refund.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.enums.RefundStatus;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @InjectMocks
    private RefundService refundService;

    @Mock
    private RefundDbProcessor refundDbProcessor;

    @Mock
    private PortOneClient portOneClient;

    private RefundRequest createRefundRequest(String orderNumber, String reason, Long orderItemId, Integer quantity) {
        RefundRequest request = new RefundRequest();
        ReflectionTestUtils.setField(request, "orderNumber", orderNumber);
        ReflectionTestUtils.setField(request, "reason", reason);

        RefundRequest.RefundItemRequest itemRequest = new RefundRequest.RefundItemRequest();
        ReflectionTestUtils.setField(itemRequest, "orderItemId", orderItemId);
        ReflectionTestUtils.setField(itemRequest, "quantity", quantity);

        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));
        return request;
    }

    @Test
    @DisplayName("🟢 [PG 환불 성공] 포트원 API 취소가 성공하면 환불 원장 상태가 COMPLETED로 최종 승인된다.")
    void refundOrder_Success_WithPgCancel() {
        // given
        Long memberId = 1L;
        RefundRequest request = createRefundRequest("ORD-2026-001", "단순 변심 환불", 100L, 2);

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPortonePaymentId()).willReturn("pay-12345");

        Refund mockRefund = mock(Refund.class);
        given(mockRefund.getId()).willReturn(100L);
        given(mockRefund.getPgRefundAmount()).willReturn(50000L);
        given(mockRefund.getPayment()).willReturn(mockPayment);

        given(refundDbProcessor.saveRefundAndRollbackStock(memberId, request)).willReturn(mockRefund);

        // when
        refundService.refundOrder(memberId, request);

        // then
        verify(portOneClient).cancelPayment("pay-12345", "단순 변심 환불", 50000L);
        // 🎯 트랜잭션 2를 통해 상태가 COMPLETED로 바뀌는지 검증 (이 내부에서 Order/OrderItem 상태 전파가 일어남)
        verify(refundDbProcessor).updateRefundStatus(100L, RefundStatus.COMPLETED);
        verify(refundDbProcessor, never()).updateRefundStatus(100L, RefundStatus.FAILED);
    }

    @Test
    @DisplayName("🟡 [전액 포인트 환불] PG 취소 금액이 0원이면 API 호출 없이 즉시 COMPLETED 처리된다.")
    void refundOrder_Success_OnlyPoint() {
        // given
        Long memberId = 1L;
        RefundRequest request = createRefundRequest("ORD-2026-002", "포인트 전액 환불", 100L, 1);

        Refund mockRefund = mock(Refund.class);
        given(mockRefund.getId()).willReturn(200L);
        given(mockRefund.getPgRefundAmount()).willReturn(0L);

        given(refundDbProcessor.saveRefundAndRollbackStock(memberId, request)).willReturn(mockRefund);

        // when
        refundService.refundOrder(memberId, request);

        // then
        verify(portOneClient, never()).cancelPayment(anyString(), anyString(), anyLong());
        verify(refundDbProcessor).updateRefundStatus(200L, RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("🚨 [PG 환불 통신 실패 대참사] 포트원 API 에러 시, 상태를 FAILED로 바꾸고 예외를 던진다.")
    void refundOrder_Fail_PortOneApiError() {
        // given
        Long memberId = 1L;
        RefundRequest request = createRefundRequest("ORD-2026-003", "서버 통신 장애 유도", 100L, 1);

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPortonePaymentId()).willReturn("pay-fail-999");

        Refund mockRefund = mock(Refund.class);
        given(mockRefund.getId()).willReturn(300L);
        given(mockRefund.getPgRefundAmount()).willReturn(30000L);
        given(mockRefund.getPayment()).willReturn(mockPayment);

        given(refundDbProcessor.saveRefundAndRollbackStock(memberId, request)).willReturn(mockRefund);

        doThrow(new RuntimeException("PortOne API Timeout"))
                .when(portOneClient).cancelPayment("pay-fail-999", "서버 통신 장애 유도", 30000L);

        // when & then
        assertThatThrownBy(() -> refundService.refundOrder(memberId, request))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_CANCEL_FAILED);

        verify(refundDbProcessor).updateRefundStatus(300L, RefundStatus.FAILED);
        verify(refundDbProcessor, never()).updateRefundStatus(300L, RefundStatus.COMPLETED);
    }
}