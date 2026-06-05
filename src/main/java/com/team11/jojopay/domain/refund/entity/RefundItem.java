package com.team11.jojopay.domain.refund.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 마스터 영수증에 종속되는 세부 환불 상품 항목 엔티티입니다.
 * 특정 주문 상품 스냅샷(OrderItemId)과 이번 회차에 환불 처리된 수량을 기록합니다.
 */
@Entity
@Table(name = "refund_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;      // 본 상세 내역을 소유하고 있는 부모 환불 마스터 엔티티

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;   // 어떤 주문 상품 항목(OrderItem)이 환불되었는지 추적하기 위한 대상 외래 식별자 ID

    @Column(name = "quantity", nullable = false)
    private Integer quantity;   // 이번 회차에 환불 처리가 완료된 단품 수량 (추후 잔여 환불 수량 집계의 기준이 됨)

    /**
     * 부모 엔티티와의 연관관계 설정을 위해 패키지 내부(default)에서만 접근을 허용하는 Setter입니다.
     *
     * @param refund 연관관계를 맺을 부모 Refund 객체
     */
    protected void setRefund(Refund refund) {
        this.refund = refund;
    }

    /**
     * 환불 상세 항목 엔티티를 생성하는 비즈니스 팩토리 메서드입니다.
     *
     * @param orderItemId 대상 주문 상품 상세 ID
     * @param quantity    환불 처리할 단품 수량
     * @return 데이터가 세팅된 RefundItem 인스턴스
     */
    public static RefundItem createRefundItem(Long orderItemId, Integer quantity) {
        RefundItem refundItem = new RefundItem();
        refundItem.orderItemId = orderItemId;
        refundItem.quantity = quantity;
        return refundItem;
    }
}
