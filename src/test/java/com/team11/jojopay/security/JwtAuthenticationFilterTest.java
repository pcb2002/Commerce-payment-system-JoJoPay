package com.team11.jojopay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import com.team11.jojopay.common.security.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock private JwtProvider jwtProvider;
    @Spy private ObjectMapper objectMapper = new ObjectMapper(); // 실제 JSON 스트링 변환 추적을 위해 Spy 할당

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext(); // 테스트 전 시큐리티 컨텍스트 초기화
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // 다음 테스트 격리를 위한 청소
    }

    @Test
    @DisplayName("필터 작동 성공: 유효한 Bearer 토큰이 헤더에 존재하면 시큐리티 컨텍스트에 사용자 식별 정보가 정상 바인딩된다.")
    void doFilterInternal_Success_ValidToken() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer valid_jojopay_token_xyz");
        given(jwtProvider.validateToken("valid_jojopay_token_xyz")).willReturn(true);
        given(jwtProvider.getMemberIdFromToken("valid_jojopay_token_xyz")).willReturn(99L);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        // 1. 시큐리티 자격증명 바인딩 여부 검증
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(99L);
        // 2. 하위 다음 필터로 온전히 통과 처리되었는지 행위 검증
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("필터 작동실패 - 만료: 토큰 검증 중 ExpiredJwtException 발생 시 401 및 TOKEN_EXPIRED 포맷 응답이 내려온다.")
    void doFilterInternal_Fail_ExpiredToken() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer expired_token_123");
        // validateToken 호출 시 강제로 만료 예외 방출 설정
        given(jwtProvider.validateToken("expired_token_123"))
                .willThrow(new ExpiredJwtException(null, null, "토큰 마감"));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        // 1. 컨텍스트가 오염되지 않고 비어있어야 함
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // 2. 응답 코드가 401(TOKEN_EXPIRED의 상태값)인지 확인
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains(ErrorCode.TOKEN_EXPIRED.name()); // JSON 에러 메세지 적재 여부 검증
        // 3. 보안 경비에 막혔으므로 다음 비즈니스 필터 체인으로 이동하지 않고 중단되었는지 검증
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("필터 우회: 헤더에 Authorization 인증 서명이 없으면 아무 작업 없이 다음 필터로 흐른다.")
    void doFilterInternal_Skip_NoHeader() throws Exception {
        // given: 헤더를 아무것도 세팅하지 않은 깡통 요청 인입

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response); // 바로 다음 정문 패스
    }
}