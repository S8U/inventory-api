package com.github.s8u.inventory.repository;

import com.github.s8u.inventory.entity.ProductStockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStockHistoryRepository extends JpaRepository<ProductStockHistory, Long> {
}
