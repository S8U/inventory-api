package com.github.s8u.inventory.usecase;

import com.github.s8u.inventory.entity.Product;
import com.github.s8u.inventory.entity.ProductStockHistory;
import com.github.s8u.inventory.enums.ProductStockHistoryType;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.repository.ProductRepository;
import com.github.s8u.inventory.repository.ProductStockHistoryRepository;
import com.github.s8u.inventory.usecase.dto.command.ProductOutboundCommand;
import com.github.s8u.inventory.usecase.dto.result.ProductOutboundResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 출고
 */
@Service
@RequiredArgsConstructor
public class ProductOutboundUseCase {

    private final ProductRepository productRepository;
    private final ProductStockHistoryRepository productStockHistoryRepository;

    @Transactional
    public ProductOutboundResult outbound(ProductOutboundCommand command) {
        // 상품 조회
        Product product = productRepository.findBySkuForUpdate(command.sku())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "sku=%s".formatted(command.sku())));

        // 재고 감소
        int beforeQuantity = product.getQuantity();
        product.decreaseQuantity(command.quantity());

        // 이력 저장
        productStockHistoryRepository.save(new ProductStockHistory(
                product.getId(),
                ProductStockHistoryType.OUTBOUND,
                command.quantity(),
                beforeQuantity,
                product.getQuantity()
        ));

        return ProductOutboundResult.from(product);
    }

}
