package com.team11.jojopay.domain.point.service;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.payment.entity.Payment;
import com.team11.jojopay.domain.point.dto.response.PointBalanceResponse;
import com.team11.jojopay.domain.point.dto.response.PointHistoryResponse;
import com.team11.jojopay.domain.point.entity.PointHistory;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import com.team11.jojopay.domain.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberService memberService;
    private final PointRepository pointRepository;

    /**
     * 본인의 현재 포인트 잔액을 조회합니다.
     *
     * @param memberId 회원 고유 식별자 ID
     * @return 현재 포인트 잔액 정보를 담은 PointBalanceResponse
     */
    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long memberId) {
        Member member = memberService.findMemberById(memberId);
        return new PointBalanceResponse(member.getPointBalance());
    }

    /**
     * 본인의 포인트 거래 내역을 전체 최신순으로 조회합니다.
     *
     * @param memberId 회원 고유 식별자 ID
     * @return 최신순으로 변환된 포인트 거래 내역 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<PointHistoryResponse> getHistories(Long memberId) {
        return pointRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream().map(PointHistoryResponse::new).collect(Collectors.toList());
    }

    /**
     * [테스트 전용] 관리자 기능이 없는 환경에서의 테스트를 위해 회원의 포인트를 수동 충전하고 원장을 기록합니다.
     *
     * @param memberId 회원 고유 식별자 ID
     * @param amount   충전할 포인트 금액
     * @return 충전 완료 후 최종 잔액 정보가 담은 PointBalanceResponse
     */
    @Transactional
    public PointBalanceResponse chargeMockPoint(Long memberId, Long amount) {
        Member member = memberService.findMemberById(memberId);

        // 1. 회원 엔티티 잔액 증가
        member.addPoint(amount);

        // 2. 포인트 이력(원장) 기록 추가 (더미 충전이므로 payment는 null 처리)
        PointHistory history = PointHistory.builder().member(member).payment(null).transactionType(PointTransactionType.EARN).amount(amount).build();
        pointRepository.save(history);

        return new PointBalanceResponse(member.getPointBalance());
    }

    @Transactional
    public void createHistory(Member member, Payment payment, PointTransactionType type, Long amount) {

        // 1. 회원의 실제 잔액 가감산 처리 (타입별 분기)
        // 💡 복구(USE_RECOVERY)면 플러스, 몰수(EARN_FORFEIT)면 마이너스 처리
        if (type == PointTransactionType.EARN || type == PointTransactionType.USE_RECOVERY) {
            member.addPoint(amount);
        } else if (type == PointTransactionType.USE || type == PointTransactionType.EARN_FORFEIT) {
            member.usePoint(amount);
        }

        // 2. PointHistory 엔티티 생성
        // 💡 이때 엔티티 내부에서 determineAmountByTransactionType()이 돌아가며 부호가 -로 이쁘게 변환되어 저장됩니다.
        PointHistory history = PointHistory.builder()
                .member(member)
                .payment(payment)
                .transactionType(type)
                .amount(amount)
                .build();

        pointRepository.save(history);
    }

}
