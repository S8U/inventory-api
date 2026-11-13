package com.github.s8u.inventory.controller.dto.response;

import com.github.s8u.inventory.usecase.dto.result.ProductInboundResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입고 응답")
public record ProductInboundResponse(
        @Schema(description = "SKU", example = "WTR-500")
        String sku,

        @Schema(description = "상품명", example = "생수 500ml")
        String name,

        @Schema(description = "입고 뒤 재고 수량", example = "10")
        int quantity
) {

    public static ProductInboundResponse from(ProductInboundResult result) {
        return new ProductInboundResponse(result.sku(), result.name(), result.quantity());
    }

}
