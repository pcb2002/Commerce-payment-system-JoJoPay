package com.team11.jojopay.domain.product.entity;

import com.team11.jojopay.common.entity.BaseTimeEntity;
import com.team11.jojopay.common.exception.ErrorCode;
import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "products")
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품명
    @Column(nullable = false, length = 255)
    private String name;

    // 판매가
    @Column(nullable = false)
    private Long price;

    // 재고 수량
    @Column(nullable = false)
    private Integer stockQuantity;

    // 상품 설명
    @Column(nullable = false, length = 1000, columnDefinition = "TEXT")
    private String description;

    // 판매 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    // 생성일시
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정일시
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품이 현재 주문 가능한 상태인지 스스로 검증합니다.
     * @param quantity 주문 요청 수량
     */
    public void validateOrderable(Integer quantity) {
        // 1. 판매 상태 검증
         if (this.status != ProductStatus.ON_SALE) {
             throw new ServiceException(ErrorCode.INVALID_PRODUCT_STATUS);
        }

        // 2. 재고 부족 검증
        if (this.stockQuantity < quantity) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    /**
     * 상품 재고를 차감합니다. (주문 생성 시 호출)
     * @param quantity 차감할 수량
     */
    public void decreaseStock(Integer quantity) {
        validateOrderable(quantity); // 차감 전 판매 상태 및 재고를 다시 한번 확실하게 검증!
        this.stockQuantity -= quantity;
    }

    /**
     * 상품 재고를 복구합니다. (주문 취소 또는 결제 실패 시 호출)
     * @param quantity 복구할 수량
     */
    public void increaseStock(Integer quantity) {
        this.stockQuantity += quantity;
    }
}
