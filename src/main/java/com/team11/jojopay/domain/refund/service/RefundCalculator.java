package com.team11.jojopay.domain.refund.service;

import lombok.Getter;

/**
 * 환불 대상 상품의 가치를 기준으로 복합 결제(포인트 + PG) 비율을 계산하고
 * 회원의 현재 잔고 상태에 맞춰 최종 정산액을 조율하는 도메인 계산기 클래스입니다.
 */
@Getter
public class RefundCalculator {

    private final long totalRefundAmount;      // 이번 회차에 환불 요청된 총 원본 상품의 가치액
    private final long pointToRestore;         // 결제 비율상 원본 기준으로 도출된 포인트 사용 복구액
    private final long pgToCancelOriginal;     // 결제 비율상 원본 기준으로 도출된 PG 취소 가치액
    private final long pointToRecoverFromEarn; // 이번 환불로 인해 회원이 반납(몰수)해야 할 구매 적립 포인트분

    private final long finalPgCancelAmount;    // 체리피커 및 잔액 방어 로직이 최종 조율하여 외부 PG사에 요청할 실 결제 취소 금액
    private final long finalPointRestoreAmount; // 조율 단계를 거쳐 회원의 지갑에 최종적으로 복구 충전해 줄 포인트 가치액

    /**
     * 최초 결제 당시의 복합 비율 스냅샷 데이터와 현재 가용 잔액을 결합하여 정산 금액을 연산합니다.
     *
     * @param totalRefundAmount         이번에 환불을 진행하는 상품 단가 수량의 총합 가치
     * @param originalTotalAmount       최초 결제 마스터의 총 주문 금액
     * @param originalUsedPoint         최초 결제 당시 사용했던 복합 포인트 수량
     * @param originalPgAmount           최초 결제 당시 신용카드 등으로 긁은 외부 PG 실결제 금액
     * @param rewardRate                환불 시점 회원의 멤버십 등급별 적립 백분율 (예: 1% 인 경우 정수 1 인입)
     * @param currentMemberPointBalance 회원의 현재 실시간 포인트 보유 잔액 원장 값
     */
    public RefundCalculator(long totalRefundAmount, long originalTotalAmount, long originalUsedPoint, long originalPgAmount, int rewardRate, long currentMemberPointBalance) {
        this.totalRefundAmount = totalRefundAmount;

        // 1. 최초 복합결제 비율 정밀 환산 처리 (소수점 누수를 방지하기 위해 double 타입 스케일 업 후 round 처리)
        double pointRatio = (double) originalUsedPoint / originalTotalAmount;
        double pgRatio = (double) originalPgAmount / originalTotalAmount;

        // 2. 환불 가치액 기준 비율별 가상 정산금 1차 도출
        this.pointToRestore = Math.round(totalRefundAmount * pointRatio);
        this.pgToCancelOriginal = Math.round(totalRefundAmount * pgRatio);
        double earnRate = rewardRate / 100.0;

        // 3. 적립 등급제 적용을 위한 백분율 전환 및 회수 대상 포인트 산정 (환불액 * 적립률)
        this.pointToRecoverFromEarn = (long) (totalRefundAmount * earnRate);

        // 4. [체리피커 방어 비즈니스 알고리즘] 회원이 이미 적립 포인트만 쏙 쓰고 환불하는 사기 차단 정책 가동
        if (currentMemberPointBalance >= this.pointToRecoverFromEarn) {
            // 케이스 A: 회원의 현재 잔액 원장이 몰수해야 할 포인트보다 크므로 포인트 잔액에서 즉시 차감 처리 가능
            this.finalPgCancelAmount = this.pgToCancelOriginal;
            this.finalPointRestoreAmount = this.pointToRestore;
        } else {
            // 케이스 B: 먹튀 발생 (잔액 부족으로 회수가 불가능한 상황)
            // 회수하지 못하고 빵꾸난 부족분 포인트 수량 역산
            long insufficientPoints = this.pointToRecoverFromEarn - currentMemberPointBalance;

            // 부족한 포인트 가치만큼 고스란히 고객이 돌려받을 현금(PG 카드 취소 대금)에서 제하고 차액만 환불해 줌으로써 플랫폼 손실 방어
            this.finalPgCancelAmount = Math.max(0, this.pgToCancelOriginal - insufficientPoints);
            this.finalPointRestoreAmount = this.pointToRestore;
        }
    }

}
