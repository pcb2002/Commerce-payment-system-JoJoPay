package com.team11.jojopay.domain.product.repository;

import com.team11.jojopay.domain.product.enums.Category;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasCategory(Category category) {
        return (root, query, cb) ->
                category == null ? cb.conjunction() // null 대신 cb.conjunction()
                        : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() // null 대신 cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> minPrice(Integer minPrice) {
        return (root, query, cb) ->
                minPrice == null ? cb.conjunction() // null 대신 cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> maxPrice(Integer maxPrice) {
        return (root, query, cb) ->
                maxPrice == null ? cb.conjunction() // null 대신 cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}