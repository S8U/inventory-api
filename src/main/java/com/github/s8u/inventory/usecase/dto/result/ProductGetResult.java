package com.github.s8u.inventory.usecase.dto.result;

import com.github.s8u.inventory.entity.Product;

public record ProductGetResult(
        String sku,
        String name,
        int quantity
) {

    public static ProductGetResult from(Product product) {
        return new ProductGetResult(product.getSku(), product.getName(), product.getQuantity());
    }

}
