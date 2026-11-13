package com.github.s8u.inventory.entity;

import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"),
        check = @CheckConstraint(name = "ck_product_quantity", constraint = "quantity >= 0"),
        comment = "상품"
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(comment = "상품 ID")
    private Long id;

    @Column(nullable = false, length = 50, comment = "SKU")
    private String sku;

    @Column(nullable = false, length = 100, comment = "상품명")
    private String name;

    @Column(nullable = false, comment = "현재 재고 수량")
    private int quantity;

    @CreatedDate
    @Column(nullable = false, updatable = false, comment = "등록 시각")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, comment = "수정 시각")
    private LocalDateTime updatedAt;

    public Product(String sku, String name) {
        this.sku = sku;
        this.name = name;
        this.quantity = 0;
    }

    /**
     * 재고를 늘린다.
     *
     * 늘린 재고가 int 최대값을 넘으면 예외를 던진다.
     */
    public void increaseQuantity(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다. (입고 요청 %d개)".formatted(quantity));
        // this.quantity + quantity로 비교하면 더하는 순간 int가 넘쳐 음수가 되므로 뺄셈으로 비교한다
        if (quantity > Integer.MAX_VALUE - this.quantity)
            throw new BusinessException(
                    ErrorCode.PRODUCT_STOCK_OVERFLOW,
                    "sku=%s, currentQuantity=%d, requestQuantity=%d".formatted(sku, this.quantity, quantity)
            );

        this.quantity += quantity;
    }

    /**
     * 재고를 줄인다.
     *
     * 재고보다 많이 줄이면 예외를 던진다.
     */
    public void decreaseQuantity(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("출고 수량은 1 이상이어야 합니다. (출고 요청 %d개)".formatted(quantity));
        if (quantity > this.quantity)
            throw new BusinessException(
                    ErrorCode.PRODUCT_STOCK_INSUFFICIENT,
                    "sku=%s, currentQuantity=%d, requestQuantity=%d".formatted(sku, this.quantity, quantity)
            );

        this.quantity -= quantity;
    }

}
