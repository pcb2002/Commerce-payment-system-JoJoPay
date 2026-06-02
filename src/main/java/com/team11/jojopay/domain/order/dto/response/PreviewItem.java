package com.team11.jojopay.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 미리보기 응답에 포함되는 개별 상품 정보 DTO입니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PreviewItem {
    private Long productId;
    private String productName;
    private Integer price; // 실시간 현재 판매가
    private Integer quantity; // 선택 수량
}