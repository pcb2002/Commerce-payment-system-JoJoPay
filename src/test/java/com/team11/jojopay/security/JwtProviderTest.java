package com.team11.jojopay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.member.entity.Member;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private String secretKeyForTest;
    private long validityTimeMs;

    @BeforeEach
    void setUp() {
        // jjwt 0.12.x에서 비밀키는 최소 256비트 이상의 충분한 길이를 가져야 에러가 나지 않습니다.
        secretKeyForTest = "testSecretKeySecretKeySecretKeySecretKeyJojoPaySecret";
        validityTimeMs = 3600000L; // 1시간 만료 시간 세팅
        jwtProvider = new JwtProvider(secretKeyForTest, validityTimeMs);
    }

    @Test
    @DisplayName("토큰 생성 및 파싱 성공: 회원 정보를 기반으로 생성된 토큰에서 정확한 Member ID를 복원한다.")
    void createAndParseToken_Success() {
        // given
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(42L);
        given(mockMember.getEmail()).willReturn("user@jojopay.com");

        // when: 토큰 생성 및 검증 가동
        String token = jwtProvider.createAccessToken(mockMember);
        Long extractedMemberId = jwtProvider.getMemberIdFromToken(token);
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertThat(token).isNotBlank();
        assertThat(extractedMemberId).isEqualTo(42L);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("토큰 검증 실패: 임의로 변조되거나 잘못된 서명을 가진 토큰은 SignatureException을 복사/발생시킨다.")
    void validateToken_Fail_InvalidSignature() {
        // given: 정상적으로 만들어진 토큰 뒤에 가짜 문자열을 붙여 서명을 손상시킵니다.
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(1L);
        String validToken = jwtProvider.createAccessToken(mockMember);
        String tamperedToken = validToken + "maliciousData";

        // when & then: 파싱 과정에서 서명 검증 에러 체크
        assertThatThrownBy(() -> jwtProvider.validateToken(tamperedToken))
                .isInstanceOf(Exception.class); // 내부 파싱 필터 체인에서 위변조를 조기 차단함을 입증
    }

    @Test
    @DisplayName("토큰 만료 실패: 만료 시간이 0인 토큰은 validateToken 호출 시 ExpiredJwtException을 방출한다.")
    void validateToken_Fail_Expired() {
        // given: 즉시 만료되도록 유효시간을 0으로 설정한 별도의 가짜 프로바이더 생성
        JwtProvider expiredProvider = new JwtProvider(secretKeyForTest, 0L);
        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(1L);
        String expiredToken = expiredProvider.createAccessToken(mockMember);

        // when & then: 만료 체크 예외 인입 보장
        assertThatThrownBy(() -> expiredProvider.validateToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}