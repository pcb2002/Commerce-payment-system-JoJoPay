package com.team11.jojopay.common.config;

import com.team11.jojopay.common.security.CustomAuthenticationEntryPoint;
import com.team11.jojopay.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 이번 프로젝트의 전역 Spring Security 보안 정책을 설정하는 인터페이스 구성 클래스입니다.
 * 무인증 접근 허용 경로(회원가입, 로그인)를 정의하고, 세션 정책을 STATELESS로 강제하며,
 * 커스텀 JWT 검증 필터 및 예외 처리 엔트리 포인트를 시큐리티 필터 체인에 바인딩합니다.
 *
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    /**
     * 회원 비밀번호를 단방향 해시 알고리즘(BCrypt)으로 안전하게 암호화하기 위한 빈(Bean)을 생성합니다.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호를 안전하게 해시 암호화하는 BCrypt 앤코더 등록
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security의 HTTP 요청 보호 및 필터 체인 순서를 구성합니다.
     *
     * @param http HttpSecurity 핵심 보안 구성 객체
     * @return 구성이 완료된 SecurityFilterChain 객체
     * @throws Exception 시큐리티 설정 중 예외 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST API이므로 csrf 보안 불필요
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용하므로 세션 미사용

                // 💡 H2 콘솔은 내부적으로 <frame> 태그를 쓰기 때문에 이 설정을 켜야 로컬 화면이 안 깨집니다.
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // API 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login", "/h2-console/**").permitAll() // 인증 제외 URL
                        .anyRequest().authenticated() // 결제, 주문 등 나머지는 전부 인증 필수
                )

                // 인증 실패 시 공통 응답 규격을 타도록 커스텀 엔트리 포인트 등록
                .exceptionHandling(exception -> exception.authenticationEntryPoint(customAuthenticationEntryPoint))

                // UsernamePasswordAuthenticationFilter 전에 커스텀 JWT 검증 필터 배치
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
