package com.team11.jojopay.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.security.CustomAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationEntryPointTest {

    @InjectMocks
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); //

    @Test
    @DisplayName("익명 사용자 차단 성공: commence 호출 시 공통 응답 상자에 UNAUTHORIZED 사유를 싣고 401 JSON을 클라이언트에 강제 응답한다.")
    void commence_Success_ReturnsFormattedJson() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        InsufficientAuthenticationException authException = new InsufficientAuthenticationException("인증 누락 에러");

        // when
        customAuthenticationEntryPoint.commence(request, response, authException);

        // then
        // 1. 상태 코드가 정밀하게 401(Unauthorized) 인지 체크
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        // 2. 미디어가 application/json 명세인지 체크
        assertThat(response.getContentType()).contains("application/json");
        // 3. 실제 내부 본문 장부에 규격 에러 정보가 포함되어 반환되는지 확인
        String jsonBody = response.getContentAsString();
        assertThat(jsonBody).contains(ErrorCode.UNAUTHORIZED.name());
    }
}