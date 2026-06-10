package com.team11.jojopay.domain.webhook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "이벤트 유형은 필수입니다.")
    private String eventType;     // 예: "PAYMENT_SUCCESS", "PAYMENT_CANCEL"

    @NotBlank(message = "포트원 결제 식별값은 필수입니다.")
    private String portonePaymentId;
}