package com.team11.jojopay.common.security;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.response.CommonApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 인증(Authentication) 정보가 없는 익명 사용자가 보호된 자원(결제, 주문, 장바구니 등)에 무단으로 접근할 시 트리거되는 커스텀 진입 엔트리 포인트입니다.
 * 시큐리티 기본 에러 화면이나 원시 401 텍스트를 반환하는 대신, 규격화된 CommonApiResponse 상자에 에러 메세지를 감싸 JSON 형태로 응답합니다.
 *
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 자격 증명이 누락된 요청에 대해 전역 표준 UNAUTHORIZED 에러 포맷으로 클라이언트에 강제 배달합니다.
     *
     * @param request       HttpServletRequest
     * @param response      HttpServletResponse
     * @param authException 보안 프레임워크 예외 오브젝트
     * @throws IOException  응답 스트림 쓰기 예외 발생 시
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonApiResponse<Void> apiResponse = CommonApiResponse.error(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
