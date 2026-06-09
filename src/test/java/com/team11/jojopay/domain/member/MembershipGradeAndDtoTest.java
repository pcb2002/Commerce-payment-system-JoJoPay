package com.team11.jojopay.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MembershipGradeAndDtoTest {

    @Test
    @DisplayName("멤버십 등급 상수 및 보상율 매칭 커버")
    void membershipGrade_Static_Values_Test() {
        // 각 상수의 getter 라인 터치
        assertThat(MembershipGrade.NORMAL.getMinimumAmount()).isEqualTo(0L);
        assertThat(MembershipGrade.NORMAL.getRewardRate()).isEqualTo(1);

        assertThat(MembershipGrade.VIP.getMinimumAmount()).isEqualTo(50000L);
        assertThat(MembershipGrade.VIP.getRewardRate()).isEqualTo(5);
    }

    @Test
    @DisplayName("차기 진급 잔여액 분기 조건문 완전 스캔")
    void calculateNextGradeRemainingAmount_Scenarios() {
        // 1. NORMAL 등급일 때
        Long normalRemains = MembershipGrade.NORMAL.calculateNextGradeRemainingAmount(25000L);
        assertThat(normalRemains).isEqualTo(25000L); // VIP(5만) - 2.5만 = 25000

        // 2. VIP 등급일 때
        Long vipRemains = MembershipGrade.VIP.calculateNextGradeRemainingAmount(60000L);
        assertThat(vipRemains).isEqualTo(40000L); // VVIP(10만) - 6만 = 40000

        // 3. 최고 등급 VVIP일 때
        Long vvipRemains = MembershipGrade.VVIP.calculateNextGradeRemainingAmount(120000L);
        assertThat(vvipRemains).isEqualTo(0L); // 최고 등급은 0L 리턴 명세 검증
    }

    @Test
    @DisplayName("MemberResponse 및 MembershipResponse 정적 팩토리 .from 컨버터 커버")
    void response_Dto_From_Entity_Test() {
        // given
        Member mockMember = mock(Member.class);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        when(mockMember.getName()).thenReturn("김유신");
        when(mockMember.getEmail()).thenReturn("kim@test.com");
        when(mockMember.getPhoneNumber()).thenReturn("010-5555-5555");
        when(mockMember.getMembershipGrade()).thenReturn(MembershipGrade.VIP);
        when(mockMember.getPointBalance()).thenReturn(500L);
        when(mockMember.getTotalPaymentAmount()).thenReturn(70000L);
        when(mockMember.getCreatedAt()).thenReturn(now);

        // when
        MemberResponse res1 = MemberResponse.from(mockMember);
        MembershipResponse res2 = MembershipResponse.from(mockMember);

        // then
        assertThat(res1.getName()).isEqualTo("김유신");
        assertThat(res1.getCreatedAt()).isEqualTo(now);

        assertThat(res2.getMembershipGrade()).isEqualTo(MembershipGrade.VIP);
        assertThat(res2.getRewardRate()).isEqualTo(5);
        assertThat(res2.getNextGradeRemainingAmount()).isEqualTo(30000L); // VVIP(10만) - 7만 = 3만
    }
}
