package com.github.s8u.inventory.usecase.dto.command;

public record ProductInboundCommand(
        String sku,
        String name,
        int quantity
) {
}
