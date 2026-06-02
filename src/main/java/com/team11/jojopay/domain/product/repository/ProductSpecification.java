package com.team11.jojopay.domain.product.repository;

import aQute.bnd.annotation.headers.Category;
import com.team11.jojopay.domain.product.entity.Product;
import com.team11.jojopay.domain.product.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    //동적 조건 검색

    // 카테고리 조건 → 카테고리에 값이 존재하면  WHERE category = ?
    // null → 없는 조건은 생략

    public static Specification<Product> hasCategory(Category category) {

        return (root, query, cb) ->

                // category가 null이면 조건 추가 안함
                category == null ? null

                        // category가 있으면
                        // category 컬럼과 값이 같은지 비교
                        : cb.equal(root.get("category"), category);
    }

    // 판매상태 조건 → WHERE status = ?

    public static Specification<Product> hasStatus(ProductStatus status) {

        return (root, query, cb) ->

                status == null ? null
                        : cb.equal(root.get("status"), status);
    }


    // 최소가격 조건  → WHERE price >= ?

    public static Specification<Product> minPrice(Integer minPrice) {

        return (root, query, cb) ->

                minPrice == null ? null

                        : cb.greaterThanOrEqualTo(

                        root.get("price"), minPrice);

    }

    // 최대가격 조건 → WHERE price <= ?

    public static Specification<Product> maxPrice(Integer maxPrice) {

        return (root, query, cb) ->

                maxPrice == null ? null

                        : cb.lessThanOrEqualTo(
                        root.get("price"), maxPrice);
    }

}



