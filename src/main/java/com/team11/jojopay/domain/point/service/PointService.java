package com.team11.jojopay.domain.point.service;

import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.service.MemberService;
import com.team11.jojopay.domain.order.validator.OrderValidator;
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
    private final OrderValidator orderValidator;

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
        Member member = orderValidator.validateAndGetMemberWithLock(memberId);

        // 1. 회원 엔티티 잔액 증가
        member.addPoint(amount);

        // 2. 포인트 이력(원장) 기록 추가 (더미 충전이므로 payment는 null 처리)
        PointHistory history = PointHistory.builder().member(member).payment(null).transactionType(PointTransactionType.EARN).amount(amount).build();
        pointRepository.save(history);

        return new PointBalanceResponse(member.getPointBalance());
    }

    /**
     * 포인트 도메인의 통합 원칙에 따라 회원의 실제 포인트 잔액을 가감하고 가동 이력을 원장에 영속화합니다.
     * 트랜잭션 타입 분기에 따라 회원(Member) 엔티티의 잔액 가감산 비즈니스 메서드를 호출하며,
     * 엔티티 내부 차감 정책에 따라 원장(PointHistory) 테이블에 부호가 정제되어 최종 저장됩니다.
     *
     * @param memberId  포인트 변동이 발생하는 대상 회원 고유 식별자 ID
     * @param payment   포인트 변동의 원인이 된 원본 결제 마스터 엔티티 (더미 충전 시 null 허용)
     * @param type      포인트 거래 유형 (EARN: 적립, USE: 사용, USE_RECOVERY: 사용분 복구, EARN_FORFEIT: 적립분 몰수)
     * @param amount    이번 이력에서 증감시킬 절대값 금액 (음수 기호 없이 양수로 전달)
     */
    @Transactional
    public void createHistory(Long memberId, Payment payment, PointTransactionType type, Long amount) {

        Member member = orderValidator.validateAndGetMemberWithLock(memberId);

        // 1. 회원의 실제 잔액 가감산 처리 (타입별 분기)
        // 복구(USE_RECOVERY) 및 적립(EARN)은 플러스 처리, 사용(USE) 및 몰수(EARN_FORFEIT)는 마이너스 처리 수행
        if (type == PointTransactionType.EARN || type == PointTransactionType.USE_RECOVERY) {
            member.addPoint(amount);
        } else if (type == PointTransactionType.USE || type == PointTransactionType.EARN_FORFEIT) {
            member.usePoint(amount);
        }

        // 2. PointHistory 엔티티 생성 및 영속화
        // 빌더 패턴을 이용하여 히스토리 인스턴스를 조립하고 리포지토리를 통해 영석화합니다.
        // 내부 도메인 규칙(determineAmountByTransactionType)에 의해 차감형 타입은 저장 시 음수로 이쁘게 정제됩니다.
        PointHistory history = PointHistory.builder()
                .member(member)
                .payment(payment)
                .transactionType(type)
                .amount(amount)
                .build();

        pointRepository.save(history);
    }

}
