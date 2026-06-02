package com.team11.jojopay.common.security;

import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.response.CommonApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;

/**
 * 매 HTTP 요청마다 Authorization 헤더를 추출하여 JWT Access Token의 유효성을 검증하는 인터셉터 성격의 보안 필터입니다.
 * OncePerRequestFilter를 구현하여 서블릿 요청당 단 1회만 수행됨을 보장하며,
 * 만료되거나 손상된 토큰의 경우 필터 내부에서 공통 API 응답 규격(CommonApiResponse)으로 예외를 즉시 클라이언트에게 반환합니다.
 *
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    /**
     * HTTP 요청을 가로채 토큰을 검증하고, 유효한 토큰일 경우 시큐리티 인증 객체를 획득하여 컨텍스트에 바인딩합니다.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @param filterChain 하위 필터 체인 제어 객체
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     * @throws IOException 입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                Long customerId = jwtProvider.getCustomerIdFromToken(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(customerId, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * HTTP Request Header에서 'Authorization' 인증 문자열을 파싱하여 순수 Bearer 토큰만 추출합니다.
     *
     * @param request HttpServletRequest
     * @return 추출된 문자열 토큰값, 규격에 맞지 않거나 누락되었을 경우 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Spring Security 컨텍스트 이전의 필터 단에서 터진 토큰 서브 예외를 전역 공통 응답 JSON 포맷으로 수동 직렬화하여 클라이언트에 응답합니다.
     *
     * @param response HttpServletResponse
     * @param errorCode 전역 실패 규격 사유 코드 (ErrorCode)
     * @throws IOException 응답 스트림 작성 도중 에러 발생 시
     */
    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonApiResponse<Void> apiResponse = CommonApiResponse.error(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
