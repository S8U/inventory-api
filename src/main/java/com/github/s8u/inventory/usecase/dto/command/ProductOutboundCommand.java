package com.github.s8u.inventory.usecase.dto.command;

public record ProductOutboundCommand(
        String sku,
        int quantity
) {
}
