package com.team11.jojopay.domain.refund.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String reason;

    @Column(name = "total_refund_amount", nullable = false)
    private Long totalRefundAmount;

    @Column(name = "point_refund_amount", nullable = false)
    private Long pointRefundAmount; // 최종 복구된 포인트 사용분

    @Column(name = "pg_refund_amount", nullable = false)
    private Long pgRefundAmount;    // 최종 포트원에 취소 요청 들어간 금액

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> refundItems = new ArrayList<>();

    public static Refund createRefund(
            Order order,
            String reason,
            long totalRefund,
            long pointRefund,
            long pgRefund
    ) {
        Refund refund = new Refund();
        refund.order = order;
        refund.reason = reason;
        refund.totalRefundAmount = totalRefund;
        refund.pointRefundAmount = pointRefund;
        refund.pgRefundAmount = pgRefund;
        return refund;
    }

    public void addRefundItem(RefundItem item) {
        this.refundItems.add(item);
        item.setRefund(this);
    }
}
