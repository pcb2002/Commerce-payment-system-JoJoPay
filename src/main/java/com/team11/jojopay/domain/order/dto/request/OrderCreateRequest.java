package com.team11.jojopay.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import java.util.List;

// 1. 주문 생성 요청 DTO (XxxCreateRequest 규칙 적용)
@Getter
public class OrderCreateRequest {
    @NotNull(message = "결제할 장바구니 아이템을 선택해주세요.")
    private List<Long> cartItemIds;

    @Min(value = 0, message = "사용 포인트는 0 이상이어야 합니다.")
    private Long usedPoint; // Wrapper 클래스 사용
}