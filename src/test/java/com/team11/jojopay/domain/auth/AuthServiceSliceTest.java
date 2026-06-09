package com.team11.jojopay.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.common.security.JwtProvider;
import com.team11.jojopay.domain.auth.dto.request.LoginRequest;
import com.team11.jojopay.domain.auth.dto.request.SignupRequest;
import com.team11.jojopay.domain.auth.dto.response.LoginResponse;
import com.team11.jojopay.domain.auth.service.AuthService;
import com.team11.jojopay.domain.member.entity.Member;
import com.team11.jojopay.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

class AuthServiceSliceTest {

    private AuthService authService;
    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtProvider = mock(JwtProvider.class);
        authService = new AuthService(memberRepository, passwordEncoder, jwtProvider);
    }

    @Test
    @DisplayName("회원가입 성공: 중복 없는 새 이메일 상신 시 암호화를 거쳐 정상 영속화 장부에 저장된다.")
    void signup_Success() throws Exception {
        // given
        String json = "{\"email\":\"newuser@test.com\",\"password\":\"password123\",\"name\":\"강감찬\",\"phoneNumber\":\"010-1111-2222\"}";
        SignupRequest request = objectMapper.readValue(json, SignupRequest.class);

        given(memberRepository.existsByEmail("newuser@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encrypted_secret_hash");

        // when
        authService.signup(request);

        // then: 내부 영속화 프록시가 작동되어 엔티티가 save 레이어로 잘 전달되었는지 추적
        verify(memberRepository, times(1)).existsByEmail("newuser@test.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("로그인 성공: 이메일과 비밀번호 정합성이 맞으면 의존성 토큰 처리를 거쳐 JWT 토큰 응답을 반환한다.")
    void login_Success() throws Exception {
        // given
        String json = "{\"email\":\"user@test.com\",\"password\":\"password123\"}";
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        Member mockMember = mock(Member.class);
        given(mockMember.getPasswordHash()).willReturn("encrypted_secret_hash");

        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches("password123", "encrypted_secret_hash")).willReturn(true);
        given(jwtProvider.createAccessToken(mockMember)).willReturn("jojopay_final_jwt_token_string");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getJwtToken()).isEqualTo("jojopay_final_jwt_token_string");
        verify(jwtProvider, times(1)).createAccessToken(mockMember);
    }

    @Test
    @DisplayName("회원가입 실패: 이미 가입된 중복 이메일 상신 시 EMAIL_DUPLICATE 커스텀 예외가 터진다.")
    void signup_Fail_EmailDuplicate() throws Exception {
        // given
        String json = "{\"email\":\"duplicate@test.com\",\"password\":\"password123\",\"name\":\"홍길동\",\"phoneNumber\":\"010-1234-5678\"}";
        SignupRequest request = objectMapper.readValue(json, SignupRequest.class);

        given(memberRepository.existsByEmail("duplicate@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.EMAIL_DUPLICATE);
                });

        verify(memberRepository, times(1)).existsByEmail("duplicate@test.com");
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 실패: 가입되어 있지 않은 유령 이메일로 접근을 시도하면 INVALID_CREDENTIALS 가 터진다.")
    void login_Fail_InvalidEmail() throws Exception {
        // given
        String json = "{\"email\":\"ghost@test.com\",\"password\":\"password123\"}";
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        given(memberRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });
    }

    @Test
    @DisplayName("로그인 실패: 이메일은 존재하나 비밀번호가 해시값과 일치하지 않으면 INVALID_CREDENTIALS 가 터진다.")
    void login_Fail_WrongPassword() throws Exception {
        // given
        String json = "{\"email\":\"user@test.com\",\"password\":\"wrong_password\"}";
        LoginRequest request = objectMapper.readValue(json, LoginRequest.class);

        Member mockMember = mock(Member.class);
        given(mockMember.getPasswordHash()).willReturn("correct_hash_value");
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(mockMember));

        // 인코더 연동 불일치 선언
        given(passwordEncoder.matches("wrong_password", "correct_hash_value")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });
    }
}
