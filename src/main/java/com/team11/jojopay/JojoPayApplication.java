package com.team11.jojopay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 도전 3단계의 스케쥴러 동작용
@EnableAsync      // 포트원 응답 주고 받을때 최적화용
@SpringBootApplication
public class JojoPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JojoPayApplication.class, args);
    }

}
