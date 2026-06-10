package com.team11.jojopay.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 오디팅 명세를 메인 클래스에서 분리하여 테스트 격리 환경을 구축하는 독립 설정 클래스입니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}