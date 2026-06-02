package com.team11.jojopay.domain.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 주문서 미리보기 응답 DTO입니다.
 * DB 저장 없이 실시간 상품 정보(스냅샷 저장 전 최신가)와 예상 결제 총액을 반환합니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class OrderPreviewResponse {
    /**
     * 장바구니에 담긴 개별 상품들의 실시간 정보 목록
     */
    private List<PreviewItem> items;

    /**
     * 모든 상품 가격과 수량을 합산한 총 주문 예상 금액
     */
    private Integer totalAmount;
}
