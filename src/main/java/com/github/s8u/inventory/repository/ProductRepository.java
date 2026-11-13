package com.github.s8u.inventory.repository;

import com.github.s8u.inventory.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * SKU로 상품을 조회하고 행을 잠근다.
     *
     * 같은 상품을 잠근 다른 트랜잭션이 있으면 그 트랜잭션이 끝날 때까지 기다린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySkuForUpdate(String sku);

    /**
     * 재고 0인 상품을 등록한다.
     *
     * 같은 SKU의 상품이 이미 있으면 아무것도 하지 않는다.
     */
    // 같은 SKU를 동시에 등록하면 한쪽만 행을 만들고, 나머지는 먼저 등록한 트랜잭션이 끝날 때까지 기다렸다가 넘어간다.
    // INSERT가 UNIQUE 위반으로 실패하면 PostgreSQL은 트랜잭션 전체를 실패 상태로 만들어 이어서 입고할 수 없으므로 충돌을 무시한다.
    @Modifying
    @Query(
            value = "INSERT INTO product (sku, name) VALUES (:sku, :name) ON CONFLICT (sku) DO NOTHING",
            nativeQuery = true
    )
    void insertIfAbsent(String sku, String name);

}
