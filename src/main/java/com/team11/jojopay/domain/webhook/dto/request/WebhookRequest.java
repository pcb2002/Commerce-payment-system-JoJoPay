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

    @NotNull(message = "결제 식별자 ID는 필수입니다.")
    private Long paymentId;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Long amount;

    @NotBlank(message = "결제 상태값은 필수입니다.")
    private String status;        // 예: "DONE", "CANCELED"
}