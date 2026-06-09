package com.team11.jojopay.domain.product.service;

import com.team11.jojopay.common.exception.ServiceException;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductServiceTest {

    @Test
    @DisplayName("주문 가능 상품")
    void validateOrderable_success() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        10,
                        "설명",
                        ProductStatus.ON_SALE,
                        Category.ELECTRONICS
                );

        assertDoesNotThrow(
                () -> product.validateOrderable(3)
        );
    }

    @Test
    @DisplayName("단종 상품 주문 불가")
    void validateOrderable_discontinued() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        10,
                        "설명",
                        ProductStatus.DISCONTINUED,
                        Category.ELECTRONICS
                );

        assertThrows(
                ServiceException.class,
                () -> product.validateOrderable(3)
        );
    }

    @Test
    @DisplayName("품절 상품 주문 불가")
    void validateOrderable_soldOut() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        10,
                        "설명",
                        ProductStatus.SOLD_OUT,
                        Category.ELECTRONICS
                );

        assertThrows(
                ServiceException.class,
                () -> product.validateOrderable(1)
        );
    }

    @Test
    @DisplayName("재고 부족")
    void validateOrderable_insufficientStock() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        3,
                        "설명",
                        ProductStatus.ON_SALE,
                        Category.ELECTRONICS
                );

        assertThrows(
                ServiceException.class,
                () -> product.validateOrderable(10)
        );
    }

    @Test
    @DisplayName("재고 차감")
    void decreaseStock_success() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        10,
                        "설명",
                        ProductStatus.ON_SALE,
                        Category.ELECTRONICS
                );

        product.decreaseStock(3);

        assertEquals(
                7,
                product.getStock()
        );
    }

    @Test
    @DisplayName("재고 복구")
    void increaseStock_success() {

        Product product =
                Product.create(
                        "맥북",
                        1000L,
                        10,
                        "설명",
                        ProductStatus.ON_SALE,
                        Category.ELECTRONICS
                );

        product.increaseStock(5);

        assertEquals(
                15,
                product.getStock()
        );
    }


}
