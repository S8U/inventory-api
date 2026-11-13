package com.github.s8u.inventory.controller.dto.request;

import com.github.s8u.inventory.usecase.dto.command.ProductInboundCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "입고 요청")
public record ProductInboundRequest(
        @Schema(description = "상품명. 처음 입고하는 SKU면 필수이고, 이미 있는 SKU면 보내도 바꾸지 않는다", example = "생수 500ml")
        @Size(max = 100, message = "상품명은 100자 이하여야 합니다")
        String name,

        @Schema(description = "입고 수량", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "수량은 필수입니다")
        @Positive(message = "수량은 1 이상이어야 합니다")
        Integer quantity
) {

    public ProductInboundCommand toCommand(String sku) {
        return new ProductInboundCommand(sku, name, quantity);
    }

}
