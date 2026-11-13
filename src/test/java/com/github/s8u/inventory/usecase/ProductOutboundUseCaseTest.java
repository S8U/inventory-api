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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductOutboundUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductStockHistoryRepository productStockHistoryRepository = mock(ProductStockHistoryRepository.class);
    private final ProductOutboundUseCase productOutboundUseCase = new ProductOutboundUseCase(
            productRepository,
            productStockHistoryRepository
    );

    @Test
    void 재고를_줄이고_출고_이력을_남긴다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.of(product(10)));

        ProductOutboundResult result = productOutboundUseCase.outbound(new ProductOutboundCommand("WTR-500", 3));

        assertEquals(new ProductOutboundResult("WTR-500", "생수 500ml", 7), result);

        ArgumentCaptor<ProductStockHistory> captor = ArgumentCaptor.forClass(ProductStockHistory.class);
        verify(productStockHistoryRepository).save(captor.capture());
        ProductStockHistory history = captor.getValue();
        assertEquals(1L, history.getProductId());
        assertEquals(ProductStockHistoryType.OUTBOUND, history.getType());
        assertEquals(3, history.getQuantity());
        assertEquals(10, history.getBeforeQuantity());
        assertEquals(7, history.getAfterQuantity());
    }

    @Test
    void 없는_SKU면_예외를_던진다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.empty());

        assertEquals(
                ErrorCode.PRODUCT_NOT_FOUND,
                assertThrows(BusinessException.class, () -> productOutboundUseCase.outbound(new ProductOutboundCommand("WTR-500", 3))).getErrorCode()
        );
        verify(productStockHistoryRepository, never()).save(any());
    }

    @Test
    void 재고보다_많이_출고하면_예외를_던지고_이력을_남기지_않는다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.of(product(10)));

        assertEquals(
                ErrorCode.PRODUCT_STOCK_INSUFFICIENT,
                assertThrows(BusinessException.class, () -> productOutboundUseCase.outbound(new ProductOutboundCommand("WTR-500", 11))).getErrorCode()
        );
        verify(productStockHistoryRepository, never()).save(any());
    }

    private Product product(int quantity) {
        Product product = new Product("WTR-500", "생수 500ml");
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "quantity", quantity);

        return product;
    }

}
