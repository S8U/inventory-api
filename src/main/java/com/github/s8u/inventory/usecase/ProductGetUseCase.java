package com.github.s8u.inventory.usecase;

import com.github.s8u.inventory.entity.Product;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.repository.ProductRepository;
import com.github.s8u.inventory.usecase.dto.result.ProductGetResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 조회
 */
@Service
@RequiredArgsConstructor
public class ProductGetUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductGetResult get(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "sku=%s".formatted(sku)));

        return ProductGetResult.from(product);
    }

}
