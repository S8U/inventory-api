package com.github.s8u.inventory.entity;

import com.github.s8u.inventory.enums.ProductStockHistoryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_stock_history",
        indexes = @Index(name = "idx_product_stock_history_product_id", columnList = "productId, id"),
        check = @CheckConstraint(name = "ck_product_stock_history_quantity", constraint = "quantity > 0"),
        comment = "재고 변동 이력"
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "재고 변동 이력 ID")
    private Long id;

    @Column(nullable = false, comment = "상품 ID")
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, comment = "변동 유형 (INBOUND/OUTBOUND)")
    private ProductStockHistoryType type;

    @Column(nullable = false, comment = "변동 수량")
    private int quantity;

    @Column(nullable = false, comment = "변동 전 재고 수량")
    private int beforeQuantity;

    @Column(nullable = false, comment = "변동 후 재고 수량")
    private int afterQuantity;

    @CreatedDate
    @Column(nullable = false, updatable = false, comment = "변동 시각")
    private LocalDateTime createdAt;

    public ProductStockHistory(
            Long productId,
            ProductStockHistoryType type,
            int quantity,
            int beforeQuantity,
            int afterQuantity
    ) {
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
    }

}
