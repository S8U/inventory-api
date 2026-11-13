package com.github.s8u.inventory.controller.dto.response;

import com.github.s8u.inventory.usecase.dto.result.ProductOutboundResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출고 응답")
public record ProductOutboundResponse(
        @Schema(description = "SKU", example = "WTR-500")
        String sku,

        @Schema(description = "상품명", example = "생수 500ml")
        String name,

        @Schema(description = "출고 뒤 재고 수량", example = "7")
        int quantity
) {

    public static ProductOutboundResponse from(ProductOutboundResult result) {
        return new ProductOutboundResponse(result.sku(), result.name(), result.quantity());
    }

}
