package com.team11.jojopay.domain.order.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티입니다.
 * 결제와 환불의 기준 단위가 되며, 생성 이후 데이터 수정 및 삭제는 불가능합니다[cite: 673].
 * 데이터베이스의 예약어(ORDER) 회피를 위해 테이블명을 'orders'로 지정합니다.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id; // 주문 ID

    @Column(nullable = false, unique = true)
    private String orderNumber; // 노출용 주문번호 (시스템 자동 생성 고유값) [cite: 644, 680]

    @Column(name = "member_id", nullable = false)
    private Long memberId; // 회원 참조 (연관관계 매핑 대신 ID만 보관하거나, 필요에 따라 @ManyToOne 사용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status; // 주문 상태 (결제대기, 주문완료, 주문취소)

    @Column(nullable = false)
    private Long totalAmount; // 주문 총액

    @Column(nullable = false)
    private Long usedPoint; // 사용 포인트

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 비즈니스 로직 및 생성자
    public static Order createOrder(Long memberId, String orderNumber, Long totalAmount, Long usedPoint) {
        Order order = new Order();
        order.memberId = memberId;
        order.orderNumber = orderNumber;
        order.totalAmount = totalAmount;
        order.usedPoint = usedPoint;
        order.status = OrderStatus.PENDING_PAYMENT; // 생성 시점에는 결제 대기 상태 [cite: 673]
        return order;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void updateTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void completeOrder() {
        this.status = OrderStatus.COMPLETED; // 결제 성공 시 호출 [cite: 673]
    }

    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED; // 결제 실패/전액 환불 시 호출 [cite: 673, 674]
    }
}