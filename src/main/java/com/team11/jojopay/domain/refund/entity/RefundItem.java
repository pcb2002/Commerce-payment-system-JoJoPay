package com.team11.jojopay.domain.refund.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Refund refund; // 환불 마스터 참조

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId; // 어떤 주문 상품 항목이 환불되었는지 스냅샷 매핑

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 이것이 기존에 이미 환불된 수량(refunded_quantity)이 됩니다.

    // 연관관계 편의 메서드용 Setter (접근 제어자 패키지 내부 default 처리)
    void setRefund(Refund refund) {
        this.refund = refund;
    }

    // 비즈니스 로직 및 생성자
    public static RefundItem createRefundItem(Long orderItemId, Integer quantity) {
        RefundItem refundItem = new RefundItem();
        refundItem.orderItemId = orderItemId;
        refundItem.quantity = quantity;
        return refundItem;
    }
}
