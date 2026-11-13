package com.github.s8u.inventory.usecase.dto.result;

import com.github.s8u.inventory.entity.Product;

public record ProductOutboundResult(
        String sku,
        String name,
        int quantity
) {

    public static ProductOutboundResult from(Product product) {
        return new ProductOutboundResult(product.getSku(), product.getName(), product.getQuantity());
    }

}
