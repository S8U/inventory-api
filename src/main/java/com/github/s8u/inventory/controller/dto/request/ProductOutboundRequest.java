package com.github.s8u.inventory.controller.dto.request;

import com.github.s8u.inventory.usecase.dto.command.ProductOutboundCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "출고 요청")
public record ProductOutboundRequest(
        @Schema(description = "출고 수량", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 1 이상이어야 합니다")
        Integer quantity
) {

    public ProductOutboundCommand toCommand(String sku) {
        return new ProductOutboundCommand(sku, quantity);
    }

}
