package com.github.s8u.inventory.usecase;

import com.github.s8u.inventory.entity.Product;
import com.github.s8u.inventory.entity.ProductStockHistory;
import com.github.s8u.inventory.enums.ProductStockHistoryType;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.repository.ProductRepository;
import com.github.s8u.inventory.repository.ProductStockHistoryRepository;
import com.github.s8u.inventory.usecase.dto.command.ProductInboundCommand;
import com.github.s8u.inventory.usecase.dto.result.ProductInboundResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 입고
 *
 * 없는 SKU면 재고 0으로 상품을 등록한 뒤 입고한다.
 * 이미 있는 SKU면 상품명을 받아도 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ProductInboundUseCase {

    private final ProductRepository productRepository;
    private final ProductStockHistoryRepository productStockHistoryRepository;

    @Transactional
    public ProductInboundResult inbound(ProductInboundCommand command) {
        // 상품 조회 (없으면 등록)
        Product product = productRepository.findBySkuForUpdate(command.sku()).orElse(null);
        if (product == null) {
            if (command.name() == null || command.name().isBlank())
                throw new BusinessException(ErrorCode.PRODUCT_NAME_REQUIRED, "sku=%s".formatted(command.sku()));

            productRepository.insertIfAbsent(command.sku(), command.name());
            product = productRepository.findBySkuForUpdate(command.sku()).orElseThrow();
        }

        // 재고 증가
        int beforeQuantity = product.getQuantity();
        product.increaseQuantity(command.quantity());

        // 이력 저장
        productStockHistoryRepository.save(new ProductStockHistory(
                product.getId(),
                ProductStockHistoryType.INBOUND,
                command.quantity(),
                beforeQuantity,
                product.getQuantity()
        ));

        return ProductInboundResult.from(product);
    }

}
