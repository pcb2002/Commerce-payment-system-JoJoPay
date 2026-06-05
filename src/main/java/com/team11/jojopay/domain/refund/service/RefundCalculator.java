package com.team11.jojopay.domain.refund.service;

import lombok.Getter;

@Getter
public class RefundCalculator {

    private final long totalRefundAmount;      // 1. 이번에 환불될 총 원본 상품 가치
    private final long pointToRestore;         // 2. 회원에게 정상적으로 돌려줄 포인트 사용분
    private final long pgToCancelOriginal;     // 3. 원래 비율대로 취소해야 할 PG 금액
    private final long pointToRecoverFromEarn; // 4. 회수해야 할 결제 당시 적립 포인트분

    // 🔥 [방식 1의 핵심] 계산 결과 실제로 포트원/회원에게 처리할 최종 조율 금액
    private final long finalPgCancelAmount;    // 최종 포트원에 취소 요청할 금액
    private final long finalPointRestoreAmount; // 최종 회원에게 복구해 줄 포인트

    // earnRate : 결제 당시 회원의 적립률 (예: 0.01)
    // currentMemberPointBalance : 현재 회원이 보유 중인 포인트 잔액
    public RefundCalculator(long totalRefundAmount, long originalTotalAmount, long originalUsedPoint, long originalPgAmount, int rewardRate, long currentMemberPointBalance) {
        this.totalRefundAmount = totalRefundAmount;

        // 1. 초기 복합결제 비율 계산 (정밀도를 위해 double 변환 후 반올림)
        double pointRatio = (double) originalUsedPoint / originalTotalAmount;
        double pgRatio = (double) originalPgAmount / originalTotalAmount;

        // 2. 기본 환불 비율 분할
        this.pointToRestore = Math.round(totalRefundAmount * pointRatio);
        this.pgToCancelOriginal = Math.round(totalRefundAmount * pgRatio);
        double earnRate = rewardRate / 100.0;

        // 3. 회수해야 할 적립 포인트 계산 (환불 금액 * 당사 적립률)
        this.pointToRecoverFromEarn = (long) (totalRefundAmount * earnRate);

        // 🔥 4. [방식 1 연산] 회원의 현재 포인트가 부족한지 검증하여 PG 금액 차감 조율
        if (currentMemberPointBalance >= this.pointToRecoverFromEarn) {
            // 케이스 A: 잔액이 충분해서 내 포인트 잔액에서 정상 차감 가능한 경우
            this.finalPgCancelAmount = this.pgToCancelOriginal;
            this.finalPointRestoreAmount = this.pointToRestore;
        } else {
            // 케이스 B: 잔액이 부족한 경우 (체리피커 방지)
            // 부족한 포인트 수량 계산
            long insufficientPoints = this.pointToRecoverFromEarn - currentMemberPointBalance;

            // 부족한 만큼 PG 취소 금액에서 까버리고, 회원은 포인트를 더 깎지 않고 0원으로 만듦
            this.finalPgCancelAmount = Math.max(0, this.pgToCancelOriginal - insufficientPoints);

            // 사용분 돌려줄 포인트가 있다면 거기서 부족분을 먼저 상쇄할 수도 있습니다.
            this.finalPointRestoreAmount = this.pointToRestore;
        }
    }

}
