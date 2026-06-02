package com.team11.jojopay.domain.product.entity;

import aQute.bnd.annotation.headers.Category;
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
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품명
    @Column(nullable = false, length = 255)
    private String name;

    // 판매가
    @Column(nullable = false)
    private Integer price;

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
}
