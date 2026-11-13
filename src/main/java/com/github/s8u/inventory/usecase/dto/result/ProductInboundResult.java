package com.github.s8u.inventory.usecase.dto.result;

import com.github.s8u.inventory.entity.Product;

public record ProductInboundResult(
        String sku,
        String name,
        int quantity
) {

    public static ProductInboundResult from(Product product) {
        return new ProductInboundResult(product.getSku(), product.getName(), product.getQuantity());
    }

}
