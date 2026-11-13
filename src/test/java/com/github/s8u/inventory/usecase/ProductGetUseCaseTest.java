package com.github.s8u.inventory.usecase;

import com.github.s8u.inventory.entity.Product;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.repository.ProductRepository;
import com.github.s8u.inventory.usecase.dto.result.ProductGetResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductGetUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductGetUseCase productGetUseCase = new ProductGetUseCase(productRepository);

    @Test
    void SKU로_상품과_현재_재고를_조회한다() {
        Product product = new Product("WTR-500", "생수 500ml");
        ReflectionTestUtils.setField(product, "quantity", 7);
        when(productRepository.findBySku("WTR-500")).thenReturn(Optional.of(product));

        ProductGetResult result = productGetUseCase.get("WTR-500");

        assertEquals(new ProductGetResult("WTR-500", "생수 500ml", 7), result);
    }

    @Test
    void 없는_SKU면_예외를_던진다() {
        when(productRepository.findBySku("WTR-500")).thenReturn(Optional.empty());

        assertEquals(
                ErrorCode.PRODUCT_NOT_FOUND,
                assertThrows(BusinessException.class, () -> productGetUseCase.get("WTR-500")).getErrorCode()
        );
    }

}
