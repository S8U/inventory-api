package com.github.s8u.inventory.controller;

import com.github.s8u.inventory.controller.dto.request.ProductInboundRequest;
import com.github.s8u.inventory.controller.dto.request.ProductOutboundRequest;
import com.github.s8u.inventory.controller.dto.response.ProductGetResponse;
import com.github.s8u.inventory.controller.dto.response.ProductInboundResponse;
import com.github.s8u.inventory.controller.dto.response.ProductOutboundResponse;
import com.github.s8u.inventory.exception.ApiErrorCodes;
import com.github.s8u.inventory.usecase.ProductGetUseCase;
import com.github.s8u.inventory.usecase.ProductInboundUseCase;
import com.github.s8u.inventory.usecase.ProductOutboundUseCase;
import com.github.s8u.inventory.usecase.dto.result.ProductGetResult;
import com.github.s8u.inventory.usecase.dto.result.ProductInboundResult;
import com.github.s8u.inventory.usecase.dto.result.ProductOutboundResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.github.s8u.inventory.exception.ErrorCode.*;

@Tag(name = "Product", description = "상품 재고")
@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {

    private static final String SKU_REGEXP = "^[A-Za-z0-9_-]{1,50}$";
    private static final String SKU_MESSAGE = "SKU는 영문, 숫자, -, _로 된 50자 이하여야 합니다";

    private final ProductInboundUseCase productInboundUseCase;
    private final ProductOutboundUseCase productOutboundUseCase;
    private final ProductGetUseCase productGetUseCase;

    @Operation(summary = "입고", description = "SKU의 재고를 늘린다. 없는 SKU면 재고 0으로 상품을 등록한 뒤 입고한다.")
    @ApiResponse(responseCode = "200", description = "입고 성공")
    @ApiErrorCodes({INVALID_REQUEST, PRODUCT_NAME_REQUIRED, PRODUCT_STOCK_OVERFLOW})
    @PostMapping(value = "/{sku}/inbound", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductInboundResponse inboundProduct(
            @Parameter(description = "SKU", example = "WTR-500")
            @PathVariable @Pattern(regexp = SKU_REGEXP, message = SKU_MESSAGE) String sku,
            @Valid @RequestBody ProductInboundRequest request
    ) {
        ProductInboundResult result = productInboundUseCase.inbound(request.toCommand(sku));
        return ProductInboundResponse.from(result);
    }

    @Operation(summary = "출고", description = "SKU의 재고를 줄인다. 재고보다 많이 출고할 수 없다.")
    @ApiResponse(responseCode = "200", description = "출고 성공")
    @ApiErrorCodes({INVALID_REQUEST, PRODUCT_NOT_FOUND, PRODUCT_STOCK_INSUFFICIENT})
    @PostMapping(value = "/{sku}/outbound", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductOutboundResponse outboundProduct(
            @Parameter(description = "SKU", example = "WTR-500")
            @PathVariable @Pattern(regexp = SKU_REGEXP, message = SKU_MESSAGE) String sku,
            @Valid @RequestBody ProductOutboundRequest request
    ) {
        ProductOutboundResult result = productOutboundUseCase.outbound(request.toCommand(sku));
        return ProductOutboundResponse.from(result);
    }

    @Operation(summary = "재고 조회", description = "SKU의 상품과 현재 재고를 조회한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorCodes({INVALID_REQUEST, PRODUCT_NOT_FOUND})
    @GetMapping("/{sku}")
    public ProductGetResponse getProductBySku(
            @Parameter(description = "SKU", example = "WTR-500")
            @PathVariable @Pattern(regexp = SKU_REGEXP, message = SKU_MESSAGE) String sku
    ) {
        ProductGetResult result = productGetUseCase.get(sku);
        return ProductGetResponse.from(result);
    }

}
