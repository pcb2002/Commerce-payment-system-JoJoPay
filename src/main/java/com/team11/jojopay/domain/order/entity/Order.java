package com.team11.jojopay.domain.order.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.order.enums.OrderItemStatus;
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
    private OrderStatus status; // 주문 상태 (결제대기, 주문완료, 주문취소, 부분환불, 전체환불)

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

    public void calculateAndValidateAmount(long totalAmount, long usedPoint) {
        this.totalAmount = totalAmount;
        this.usedPoint = usedPoint;

        long pgRealAmount = totalAmount - usedPoint;

        if (pgRealAmount < 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
    }

    public void completeOrder() {
        this.status = OrderStatus.COMPLETED; // 결제 성공 시 호출 [cite: 673]
    }

    public void cancelOrder() {
        // 1. 이미 결제가 완료되었거나 환불 프로세스를 탄 주문은 단순 '결제 취소'를 할 수 없음 (환불 전용 로직으로 가야 함)
        if (this.status == OrderStatus.COMPLETED ||
                this.status == OrderStatus.PARTIAL_REFUND ||
                this.status == OrderStatus.FULLY_REFUNDED) {
            throw new ServiceException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // 2. 이미 결제 취소 처리가 완료된 주문인 경우 방어
        if (this.status == OrderStatus.CANCELLED) {
            throw new ServiceException(ErrorCode.ORDER_ALREADY_BE_CANCELLED);
        }

        // 3. 결제 대기(PENDING_PAYMENT) 상태에서 정상적으로 결제 취소(CANCELLED) 종결
        this.status = OrderStatus.CANCELLED;
    }

    public void updateStatusByItems() {
        if (this.orderItems == null || this.orderItems.isEmpty()) {
            return;
        }

        // 모든 하위 상품이 REFUNDED 상태인지 확인
        boolean allRefunded = this.orderItems.stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.REFUNDED);

        // 하나라도 REFUNDED 상태가 있는지 확인
        boolean anyRefunded = this.orderItems.stream()
                .anyMatch(item -> item.getStatus() == OrderItemStatus.REFUNDED);

        // 하위 상품들의 환불 상태를 기반으로 주문 원장 상태 스위칭
        if (allRefunded) {
            this.status = OrderStatus.FULLY_REFUNDED; // 전액 환불 확정
        } else if (anyRefunded) {
            this.status = OrderStatus.PARTIAL_REFUND; // 일부 환불 확정
        }
    }
}