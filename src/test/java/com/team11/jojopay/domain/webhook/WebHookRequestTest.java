package com.team11.jojopay.domain.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.team11.jojopay.domain.webhook.dto.request.WebhookRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebhookRequestTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        // 자바 표준 규격의 밸리데이터 팩토리를 로드하여 테스트 환경에 격리 이식합니다.
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("WebhookRequest 성공 검증")
    void webhookRequest_Validation_Success() {
        // given
        WebhookRequest request = new WebhookRequest("PAYMENT_SUCCESS", 1L, 10000L, "DONE");

        // when: 유효성 검증 수행 및 Getter 호출 라인 커버
        Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

        // then: 에러가 전혀 없음을 단언
        assertThat(violations).isEmpty();
        assertThat(request.getEventType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(request.getPaymentId()).isEqualTo(1L);
        assertThat(request.getAmount()).isEqualTo(10000L);
        assertThat(request.getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("WebhookRequest 실패 검증")
    void webhookRequest_Validation_Fail_NotBlank_And_IsNull() {
        // given: 필수값들을 전부 누락(null 또는 공백)시킨 위험한 상태의 DTO 조립
        WebhookRequest invalidRequest = new WebhookRequest(" ", null, null, "");

        // when: 유효성 검증 수행
        Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(invalidRequest);

        // then: 제약 조건 위반이 정확히 4개 적발되었는지 검증하여 사각지대 전면 제거
        assertThat(violations).hasSize(4);
    }
}
