package com.team11.jojopay.domain.product.repository;

import com.team11.jojopay.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JpaSpecificationExecutor<Product>를 상속받음으로써
// findAll(Specification<Product> spec, Pageable pageable) 같은 동적 검색 메서드를 사용할 수 있게 됨.
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // 기본 제공되는 findById를 사용하셔도 되지만,
    // 커머스 특성상 동시성(따닥 결제) 문제가 발생할 수 있으므로
    // 실제 결제용 재고 차감을 위한 '비관적 락(Pessimistic Lock)' 메서드를 하나 만들어 두는 것을 강력히 추천합니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}
