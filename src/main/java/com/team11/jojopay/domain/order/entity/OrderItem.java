package com.team11.jojopay.domain.order.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id; // 주문 상품 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // 주문 참조

    @Column(name = "product_id", nullable = false)
    private Long productId; // 상품 참조

    @Column(nullable = false)
    private String productName; // 상품명 스냅샷

    @Column(nullable = false)
    private Long priceAtOrder; // 주문 생성 시점 가격 스냅샷

    @Column(nullable = false)
    private Integer quantity; // 수량

    // 연관관계 편의 메서드용 Setter (접근 제어자 default 처리)
    void setOrder(Order order) {
        this.order = order;
    }

    // 비즈니스 로직 및 생성자
    public static OrderItem createOrderItem(Long productId, String productName, Long priceAtOrder, Integer quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.productId = productId;
        orderItem.productName = productName;
        orderItem.priceAtOrder = priceAtOrder;
        orderItem.quantity = quantity;
        return orderItem;
    }
}