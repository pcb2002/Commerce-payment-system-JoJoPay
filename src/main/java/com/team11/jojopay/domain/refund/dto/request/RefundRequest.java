package com.team11.jojopay.domain.refund.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 클라이언트로부터 환불 요청을 받기 위한 데이터 전송 객체(DTO)입니다.
 * 비즈니스 식별자인 주문 번호와 환불 사유, 그리고 부분 환불을 지원하기 위한
 * 상세 상품 항목 리스트를 캡슐화합니다.
 */
@Getter
public class RefundRequest {

    @NotBlank(message = "주문 번호는 필수 입력 값입니다.")
    private String orderNumber;                 // 환불을 진행할 대상 주문의 고유 비즈니스 키 (주문 번호)

    @NotBlank(message = "환불 사유는 필수 입력 값입니다.")
    private String reason;                      // 고객이 작성한 환불 처리 사유 (PG사 송출 및 데이터베이스 기록용)

    @NotEmpty(message = "환불할 상품 항목은 최소 1개 이상이어야 합니다.")
    private List<RefundItemRequest> items;      // 환불 대상이 되는 주문 상품 항목 및 수량 정보를 담은 컬렉션

    /**
     * 환불 대상 상품의 식별자와 수량을 바인딩하기 위한 내부 정적 클래스입니다.
     */
    @Getter
    @NoArgsConstructor
    public static class RefundItemRequest {
        private Long orderItemId;   // 주문 상세 상품 항목 테이블(OrderItem)의 고유 식별자 PK
        private Integer quantity;   // 해당 항목에서 환불하고자 하는 요청 수량
    }

}
