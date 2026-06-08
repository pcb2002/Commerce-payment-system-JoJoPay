package com.team11.jojopay.domain.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문서 미리보기 요청 DTO입니다.
 * 클라이언트가 결제하려는 장바구니 아이템의 ID 목록을 전달합니다.
 */
@Getter
@NoArgsConstructor
public class OrderPreviewRequest {
    /**
     * 결제할 장바구니 상품 ID 목록
     * 값이 비어있을 경우 예외를 발생시키거나 전체 장바구니를 조회하도록 처리할 수 있습니다.
     */
    @NotNull(message = "장바구니 아이템 ID 목록은 필수입니다.")
    private List<Long> cartItemIds;
}
