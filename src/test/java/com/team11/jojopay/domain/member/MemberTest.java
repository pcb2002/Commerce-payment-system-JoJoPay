package com.team11.jojopay.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("회원 도메인 비즈니스 갱신 로직 검증: 결제액 증감에 따라 등급 자동 트리거 정책이 구동된다.")
    void member_Domain_Business_Test() {
        // given: 최초 가입 시 NORMAL 등급 기본값 안착
        Member member = Member.signup("이순신", "lee@test.com", "hash", "010-3333-5555");
        assertThat(member.getMembershipGrade()).isEqualTo(MembershipGrade.NORMAL);

        // 1. VIP 기준선(50,000원) 돌파 유도
        member.increaseTotalPaymentAmount(55000L);
        assertThat(member.getMembershipGrade()).isEqualTo(MembershipGrade.VIP);

        // 2. VVIP 기준선(100,000원) 누적 돌파 유도
        member.increaseTotalPaymentAmount(50000L); // 55000 + 50000 = 105,000원
        assertThat(member.getMembershipGrade()).isEqualTo(MembershipGrade.VVIP);

        // 3. 환불로 인한 등급 하향 전이 검증
        member.decreaseTotalPaymentAmount(60000L); // 105000 - 60000 = 45,000원 (NORMAL 복귀)
        assertThat(member.getMembershipGrade()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("포인트 원장 제어권 검증: 충전은 제한이 없으나 잔액보다 많은 금액을 쓰려고 하면 예외가 발생한다.")
    void member_Point_Wallet_Test() {
        // given
        Member member = Member.signup("유관순", "yu@test.com", "hash", "010-7777-8888");

        // 포인트 충전 라인 관통
        member.addPoint(3000L);
        assertThat(member.getPointBalance()).isEqualTo(3000L);

        // 포인트 정상 차감
        member.usePoint(1000L);
        assertThat(member.getPointBalance()).isEqualTo(2000L);

        // 잔액 부족(2000원 보유 중인데 5000원 사용 신청) 에러 구문 타격
        assertThatThrownBy(() -> member.usePoint(5000L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                });
    }
}
