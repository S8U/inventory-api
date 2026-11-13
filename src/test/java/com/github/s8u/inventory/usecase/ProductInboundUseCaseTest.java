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

class ProductInboundUseCaseTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductStockHistoryRepository productStockHistoryRepository = mock(ProductStockHistoryRepository.class);
    private final ProductInboundUseCase productInboundUseCase = new ProductInboundUseCase(
            productRepository,
            productStockHistoryRepository
    );

    @Test
    void 있는_SKU면_재고를_늘리고_입고_이력을_남긴다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.of(product(10)));

        ProductInboundResult result = productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", null, 5));

        assertEquals(new ProductInboundResult("WTR-500", "생수 500ml", 15), result);
        assertHistory(ProductStockHistoryType.INBOUND, 5, 10, 15);
        verify(productRepository, never()).insertIfAbsent(any(), any());
    }

    @Test
    void 있는_SKU면_상품명을_받아도_바꾸지_않는다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.of(product(10)));

        ProductInboundResult result = productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", "다른 이름", 5));

        assertEquals("생수 500ml", result.name());
    }

    @Test
    void 없는_SKU면_상품을_등록한_뒤_입고한다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.empty(), Optional.of(product(0)));

        ProductInboundResult result = productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", "생수 500ml", 5));

        verify(productRepository).insertIfAbsent("WTR-500", "생수 500ml");
        assertEquals(new ProductInboundResult("WTR-500", "생수 500ml", 5), result);
        assertHistory(ProductStockHistoryType.INBOUND, 5, 0, 5);
    }

    @Test
    void 등록하는_사이_다른_요청이_먼저_등록했으면_그_재고에_더한다() {
        // 처음 조회할 때는 없었지만, 그 사이 같은 SKU의 다른 입고가 먼저 상품을 만들고 10개를 넣은 경우
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.empty(), Optional.of(product(10)));

        ProductInboundResult result = productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", "생수 500ml", 5));

        assertEquals(15, result.quantity());
        assertHistory(ProductStockHistoryType.INBOUND, 5, 10, 15);
    }

    @Test
    void 없는_SKU인데_상품명이_없으면_예외를_던지고_등록하지_않는다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.empty());

        assertEquals(
                ErrorCode.PRODUCT_NAME_REQUIRED,
                assertThrows(BusinessException.class, () -> productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", null, 5))).getErrorCode()
        );
        assertEquals(
                ErrorCode.PRODUCT_NAME_REQUIRED,
                assertThrows(BusinessException.class, () -> productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", " ", 5))).getErrorCode()
        );
        verify(productRepository, never()).insertIfAbsent(any(), any());
        verify(productStockHistoryRepository, never()).save(any());
    }

    @Test
    void 재고가_최대값을_넘으면_예외를_던지고_이력을_남기지_않는다() {
        when(productRepository.findBySkuForUpdate("WTR-500")).thenReturn(Optional.of(product(Integer.MAX_VALUE)));

        assertEquals(
                ErrorCode.PRODUCT_STOCK_OVERFLOW,
                assertThrows(BusinessException.class, () -> productInboundUseCase.inbound(new ProductInboundCommand("WTR-500", null, 1))).getErrorCode()
        );
        verify(productStockHistoryRepository, never()).save(any());
    }

    private Product product(int quantity) {
        Product product = new Product("WTR-500", "생수 500ml");
        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "quantity", quantity);

        return product;
    }

    private void assertHistory(ProductStockHistoryType type, int quantity, int beforeQuantity, int afterQuantity) {
        ArgumentCaptor<ProductStockHistory> captor = ArgumentCaptor.forClass(ProductStockHistory.class);
        verify(productStockHistoryRepository).save(captor.capture());

        ProductStockHistory history = captor.getValue();
        assertEquals(1L, history.getProductId());
        assertEquals(type, history.getType());
        assertEquals(quantity, history.getQuantity());
        assertEquals(beforeQuantity, history.getBeforeQuantity());
        assertEquals(afterQuantity, history.getAfterQuantity());
    }

}
