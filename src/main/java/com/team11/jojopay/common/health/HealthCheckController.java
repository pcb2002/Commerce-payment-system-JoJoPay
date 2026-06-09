package com.team11.jojopay.common.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        // 서버가 정상적으로 켜져 있다면 "OK"를 반환합니다.
        return ResponseEntity.ok("OK");
    }
}