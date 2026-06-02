package com.team11.jojopay.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 조조페이(JojoPay) 플랫폼의 JWT 생성, 파싱 및 무결성 검증을 전담하는 컴포넌트입니다.
 * 외부 Properties 클래스 분리 없이 환경 변수 Value와 최신 규격의 SecretKey를
 * 하나의 파일에서 응집력 있게 관리합니다.
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidTime;

    // 💡 생성자 시점에 application.properties 설정을 직접 주입받아 합칩니다.
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-valid-time}") long accessTokenValidTime
    ) {
        this.accessTokenValidTime = accessTokenValidTime;
        // 주입받은 secret 문자열을 최신 jjwt 표준인 SecretKey 객체로 즉시 변환하여 보관
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 로그인 성공 시 조조페이 유저(Member)의 식별값과 이메일을 담은 Access Token을 생성합니다.
     */
    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + accessTokenValidTime);

        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("email", member.getEmail())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰의 subject에 저장된 고유 Customer ID를 꺼냅니다.
     */
    public Long getCustomerIdFromToken(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    /**
     * 토큰의 만료/위변조 여부를 검증합니다.
     * 파싱 중 에러 발생 시 JwtAuthenticationFilter의 try-catch 블록이 가로채어
     * 커스텀 공통 에러 응답(TOKEN_EXPIRED 등)으로 처리합니다.
     */
    public boolean validateToken(String token) {
        getClaims(token);
        return true;
    }

    /**
     * 최신 jjwt 0.12.x 스펙에 맞춰 서명과 만료 시간을 정밀 검증하고 페이로드를 반환합니다.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
