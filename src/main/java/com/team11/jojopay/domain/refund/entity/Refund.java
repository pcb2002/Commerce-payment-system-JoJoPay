package com.team11.jojopay.domain.refund.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 환불 원장 마스터 영수증 정보를 관리하는 도메인 엔티티입니다.
 * 하나의 주문(Order)에 대해 발생한 최종 환불 총액, 포인트 복구액,
 * 그리고 PG 실결제 취소 대금을 영속화하며, 상세 환불 아이템 목록을 관리합니다.
 */
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
    private String reason;              // 환불 처리에 대한 사유 내용

    @Column(name = "total_refund_amount", nullable = false)
    private Long totalRefundAmount;     // 결제 단가와 환불 수량을 곱하여 산정된 순수 상품 가치의 총합 환불액

    @Column(name = "point_refund_amount", nullable = false)
    private Long pointRefundAmount;     // 도메인 정산 연산을 통해 회원에게 최종적으로 돌려준(충전된) 포인트 사용분 금액

    @Column(name = "pg_refund_amount", nullable = false)
    private Long pgRefundAmount;        // 포트원(PortOne) API를 통해 카드/계좌 사로 실제 승인 취소 요청이 들어간 금액

    /**
     * 환불 영수증에 종속된 세부 환불 상품 항목 리스트 (영속성 전이 및 고아 객체 제거 활성화)
     */
    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> refundItems = new ArrayList<>();

    /**
     * 환불 마스터 엔티티를 생성하는 안전한 팩토리 메서드입니다.
     *
     * @param order       대상 주문 엔티티 객체
     * @param reason      환불 사유
     * @param totalRefund 원본 상품 가치 기준 환불 총액
     * @param pointRefund 최종 조율된 포인트 복구액
     * @param pgRefund    최종 조율된 외부 PG 실취소액
     * @return 속성 주입이 완료된 Refund 인스턴스
     */
    public static Refund createRefund(Order order, String reason, long totalRefund, long pointRefund, long pgRefund) {
        Refund refund = new Refund();
        refund.order = order;
        refund.reason = reason;
        refund.totalRefundAmount = totalRefund;
        refund.pointRefundAmount = pointRefund;
        refund.pgRefundAmount = pgRefund;
        return refund;
    }

    /**
     * 환불 마스터 영수증과 자식 상세 항목 간의 양방향 연관관계를 설정하는 편의 메서드입니다.
     *
     * @param item 환불 영수증에 결합할 자식 상세 항목 엔티티(RefundItem)
     */
    public void addRefundItem(RefundItem item) {
        this.refundItems.add(item);
        item.setRefund(this);
    }
}
