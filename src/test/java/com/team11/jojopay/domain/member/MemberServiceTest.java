package com.team11.jojopay.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.member.dto.response.MemberResponse;
import com.team11.jojopay.domain.member.dto.response.MembershipResponse;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.enums.MembershipGrade;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import com.team11.jojopay.domain.member.service.MemberService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    private Member mockMember;

    @BeforeEach
    void setUp() {
        mockMember = mock(Member.class);
    }

    @Test
    @DisplayName("회원 기본 정보 단건 조회 성공")
    void getMyInfo_Success() {
        // given
        when(mockMember.getName()).thenReturn("홍길동");
        when(mockMember.getMembershipGrade()).thenReturn(MembershipGrade.NORMAL);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));

        // when
        MemberResponse response = memberService.getMyInfo(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("홍길동");
        verify(memberRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("회원 정보 조회 실패: 존재하지 않는 가상 ID 상신 시 MEMBER_NOT_FOUND 예외가 방출된다.")
    void getMyInfo_Fail_NotFound() {
        // given
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(99L))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("멤버십 등급 정보 조회 성공")
    void getMyMembership_Success() {
        // given
        when(mockMember.getMembershipGrade()).thenReturn(MembershipGrade.VIP);
        when(mockMember.getTotalPaymentAmount()).thenReturn(55000L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));

        // when
        MembershipResponse response = memberService.getMyMembership(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMembershipGrade()).isEqualTo(MembershipGrade.VIP);
    }

    @Test
    @DisplayName("내부 간접 연동 findMemberById 성공")
    void findMemberById_Success() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));

        // when
        Member result = memberService.findMemberById(1L);

        // then
        assertThat(result).isEqualTo(mockMember);
    }
}
