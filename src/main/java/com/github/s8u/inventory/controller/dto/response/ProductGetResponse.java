package com.github.s8u.inventory.controller.dto.response;

import com.github.s8u.inventory.usecase.dto.result.ProductGetResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재고 조회 응답")
public record ProductGetResponse(
        @Schema(description = "SKU", example = "WTR-500")
        String sku,

        @Schema(description = "상품명", example = "생수 500ml")
        String name,

        @Schema(description = "현재 재고 수량", example = "7")
        int quantity
) {

    public static ProductGetResponse from(ProductGetResult result) {
        return new ProductGetResponse(result.sku(), result.name(), result.quantity());
    }

}
