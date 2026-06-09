package com.team11.jojopay.domain.refund.service;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.refund.dto.request.RefundRequest;
import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.enums.RefundStatus;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundDbProcessor refundDbProcessor;
    private final PortOneClient portOneClient;

    /**
     * 외부 PG 호출을 완전히 격리하여 금융권 스펙의 정정합성 환불 오케스트레이션을 완수합니다.
     */
    public void refundOrder(Long memberId, RefundRequest request) {

        // [Step 1] 내부 검증 및 DB 선커밋 (상태: READY)
        // 이 단계가 완료되면 재고 복구, 포인트 가감산 및 READY 영수증이 완벽히 고정됩니다.
        Refund savedRefund = refundDbProcessor.saveRefundAndRollbackStock(memberId, request);

        // [Step 2] 트랜잭션 외부: 외부 PG사(PortOne) API 호출 실결제 취소 발송
        if (savedRefund != null && savedRefund.getPgRefundAmount() > 0) {

            // 데이터 변경 유실 방지를 위해 주문번호 기반의 PortOne 결제 식별자 추출
            String portonePaymentId = savedRefund.getPayment().getPortonePaymentId();

            try {
                portOneClient.cancelPayment(
                        portonePaymentId,
                        request.getReason(),
                        savedRefund.getPgRefundAmount()
                );
                log.info("[포트원 PG 환불 성공] 거래키: {}, 취소액: {}원", portonePaymentId, savedRefund.getPgRefundAmount());

                // [Step 3-A] API 성공 시 최종 상태를 'COMPLETED'로 확정 치기
                refundDbProcessor.updateRefundStatus(savedRefund.getId(), RefundStatus.COMPLETED);

            } catch (Exception e) {
                // [Step 3-B] API 실패 시 상태를 'FAILED'로 변경하고 기획 조건대로 로그 보강
                // 이 영역이 실행되더라도 Step 1에서 처리한 재고 및 포인트 데이터가 롤백되어 날아가지 않습니다.
                refundDbProcessor.updateRefundStatus(savedRefund.getId(), RefundStatus.FAILED);

                log.error("[포트원 환불 통신 실패 대참사 발생] 비즈니스 원장은 저장되었으나 실결제 취소 통신 에러");
                log.error("수동 정산 대상 리스트 점검 요망 -> 환불 원장 ID: {}, 포트원 거래키: {}, 실패 취소액: {}원",
                        savedRefund.getId(), portonePaymentId, savedRefund.getPgRefundAmount(), e);

                throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }
        } else {
            // 전액 포인트 환불 등 PG 취소액이 없는 경우, 바로 성공 처리 종결
            refundDbProcessor.updateRefundStatus(savedRefund.getId(), RefundStatus.COMPLETED);
            log.info("[포인트 전액 환불 성공] 환불 원장 ID: {}, PG 취소액이 없으므로 즉시 완료 처리되었습니다.", savedRefund.getId());
        }
    }
}