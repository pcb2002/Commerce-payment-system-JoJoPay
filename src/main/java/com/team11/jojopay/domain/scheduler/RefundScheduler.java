package com.team11.jojopay.domain.scheduler;

import com.team11.jojopay.domain.payment.dto.response.PortOnePaymentResponse;
import com.team11.jojopay.domain.refund.entity.Refund;
import com.team11.jojopay.domain.refund.enums.RefundStatus;
import com.team11.jojopay.domain.refund.repository.RefundRepository;
import com.team11.jojopay.domain.refund.service.RefundDbProcessor;
import com.team11.jojopay.infrastructure.portone.client.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundScheduler {

    private final RefundRepository refundRepository;
    private final RefundDbProcessor refundDbProcessor;
    private final PortOneClient portOneClient;

    /**
     * 매일 자정에 가동되어 내부 DB에는 READY(취소대기)로 선저장되었으나
     * 외부 네트워크 문제나 서버 다운으로 인해 사후 확정을 짓지 못한 낙오자 환불 원장들을 자동으로 구제합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resolveStrandedRefunds() {
        log.info("[환불 배치 스케줄러] 시스템 다운 및 네트워크 유실로 인한 낙오 데이터 정밀 스캔 가동...");

        // 안전 장치: 현재 시간 기준 생성된 지 최소 60분이 지난 READY 상태의 데이터만 타깃으로 삼습니다.
        // (실시간으로 처리 중인 정상적인 환불 건을 배치가 건드리는 간섭 현상을 방지하기 위함)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(60);
        List<Refund> strandedRefunds = refundRepository.findAllByStatusAndCreatedAtBefore(RefundStatus.READY, threshold);

        if (strandedRefunds.isEmpty()) {
            log.info("[환불 배치 스케줄러] 구제 대상 낙오 데이터가 존재하지 않습니다. 깔끔한 상태입니다.");
            return;
        }

        log.info("[환불 배치 스케줄러] 총 {}건의 낙오 의심 데이터를 발견했습니다. 전수 검증을 시작합니다.", strandedRefunds.size());

        for (Refund refund : strandedRefunds) {
            // 연관된 마스터를 타고 들어가 포트원 결제 식별자 획득
            String portonePaymentId = refund.getPayment().getPortonePaymentId();

            try {
                // 1. 🔍 보낸주신 PortOneClient의 실제 메서드를 통해 포트원 서버 측 진짜 결제 상태 정보를 획득합니다.
                PortOnePaymentResponse pgInfo = portOneClient.getPaymentInfo(portonePaymentId);

                // 2. 포트원 측 응답 객체의 상태 값이나 환불/취소 내역 필드를 검증합니다.
                // 💡 (주의) PortOnePaymentResponse 내의 정확한 취소 상태 필드명(예: getStatus() 등)에 맞춰 조건을 바인딩하세요.
                // 여기서는 예시로 포트원 결제 상태가 "CANCELLED"(취소됨) 상태이거나
                // 혹은 이미 취소 금액 처리가 포트원 대시보드에 반영되어 있는지 확인하는 분기문입니다.
                if (pgInfo != null && "CANCELLED".equals(pgInfo.getStatus())) {

                    // [케이스 A] 포트원 서버에는 이미 취소가 성공해 있는 상태인 경우:
                    // 우리 서버가 무전을 치고 응답을 받기 직전에 뻗었던 것이므로 우리 DB 상태만 정상으로 맞춰줍니다.
                    refundDbProcessor.updateRefundStatus(refund.getId(), RefundStatus.COMPLETED);
                    log.info("[배치 보정 완효] 포트원사 취소 사실 확인 완료 -> 내부 DB COMPLETED 동기화 완료 (환불 ID: {})", refund.getId());
                    continue;
                }

                // [케이스 B] 포트원 서버에도 취소가 안 들어가 있는 상태인 경우:
                // 우리 서버가 1단계 DB만 커밋하고 2단계 무전을 치기도 전에 서버가 다운된 진성 낙오 건입니다.
                // 우리가 만들어둔 안전한 취소 무전기(cancelPayment)를 통해 재시도 신호를 송출합니다.
                if (refund.getPgRefundAmount() > 0) {
                    portOneClient.cancelPayment(
                            portonePaymentId,
                            refund.getReason() + " (시스템 다운으로 인한 스케줄러 자동 재시도)",
                            refund.getPgRefundAmount()
                    );
                }

                // 성공적으로 재취소 무전이 나가면 깔끔하게 COMPLETED로 확정
                refundDbProcessor.updateRefundStatus(refund.getId(), RefundStatus.COMPLETED);
                log.info("[배치 구제 완료] 포트원 취소 재시도 성공 -> 최종 완료 마킹 (환불 ID: {})", refund.getId());

            } catch (Exception e) {
                // 배치 로봇이 자동 재시도를 돌렸음에도 불구하고 포트원 한도 초과 오류나 네트워크 거부 등으로 다시 튕긴 최악의 경우입니다.
                // 이 건은 진짜 시스템이 자동으로 해결할 수 없는 마지노선이므로 FAILED 상태로 강제 전환시켜 관리자 어드민 페이지에 빨간 불이 켜지도록 유도합니다.
                refundDbProcessor.updateRefundStatus(refund.getId(), RefundStatus.FAILED);

                log.error("[🚨 배치 자동 구제 실패] 외부 통신 재시도 실패. 수동 확인이 시급합니다.", e);
                log.error("미해결 낙오 환불 원장 ID: {}, 포트원 TID: {}, 금액: {}원",
                        refund.getId(), portonePaymentId, refund.getPgRefundAmount());
            }
        }
    }
}
