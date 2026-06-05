package com.team11.jojopay.domain.refund.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class RefundRequest {

    @NotBlank(message = "주문 번호는 필수 입력 값입니다.")
    private String orderNumber;

    @NotBlank(message = "환불 사유는 필수 입력 값입니다.")
    private String reason;

    @NotEmpty(message = "환불할 상품 항목은 최소 1개 이상이어야 합니다.")
    private List<RefundItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class RefundItemRequest {
        private Long orderItemId;
        private Integer quantity;
    }

}
