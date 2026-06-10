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
        // 자바 빈 빌리데이션 명세를 단독 실행하기 위해 로컬 팩토리 머신 활성화
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("DTO 유효성 검증: 정상적인 이벤트 유형과 거래 식별자가 들어오면 위반 제약조건이 0개여야 한다.")
    void webhookRequest_Validation_Success() {
        // given
        WebhookRequest request = new WebhookRequest("PAYMENT_CANCEL", "portone-test-id-1234");

        // when
        Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty(); // 어떠한 보안 위반 사항도 없어야 함
    }

    @Test
    @DisplayName("DTO 제약 조건 위반 검증: eventType에 빈 값이 인입되면 @NotBlank 어노테이션 규칙에 의해 에러 목록이 적재된다.")
    void webhookRequest_Validation_NotBlank_EventType() {
        // given: eventType이 공백 문자열인 상황
        WebhookRequest request = new WebhookRequest(" ", "portone-test-id-1234");

        // when
        Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("이벤트 유형은 필수입니다.");
    }

    @Test
    @DisplayName("DTO 제약 조건 위반 검증: portonePaymentId가 완전히 null인 유령 객체가 인입되면 빈 입력값 제약조건에 걸린다.")
    void webhookRequest_Validation_NotBlank_PortoneId() {
        // given: portonePaymentId 정보 누락 상황
        WebhookRequest request = new WebhookRequest("PAYMENT_SUCCESS", null);

        // when
        Set<ConstraintViolation<WebhookRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("포트원 결제 식별값은 필수입니다.");
    }
}