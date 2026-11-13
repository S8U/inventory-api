package com.github.s8u.inventory.entity;

import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void 새_상품은_재고_0으로_시작한다() {
        Product product = new Product("WTR-500", "생수 500ml");

        assertEquals(0, product.getQuantity());
    }

    @Nested
    class IncreaseQuantity {

        @Test
        void 재고를_늘리면_수량이_더해진다() {
            Product product = product(10);

            product.increaseQuantity(5);

            assertEquals(15, product.getQuantity());
        }

        @Test
        void 수량이_0_이하면_예외를_던지고_재고는_그대로다() {
            Product product = product(10);

            assertThrows(IllegalArgumentException.class, () -> product.increaseQuantity(0));
            assertThrows(IllegalArgumentException.class, () -> product.increaseQuantity(-1));
            assertEquals(10, product.getQuantity());
        }

        @Test
        void 늘린_재고가_int_최대값과_같으면_늘어난다() {
            Product product = product(Integer.MAX_VALUE - 1);

            product.increaseQuantity(1);

            assertEquals(Integer.MAX_VALUE, product.getQuantity());
        }

        @Test
        void 늘린_재고가_int_최대값을_넘으면_예외를_던지고_재고는_그대로다() {
            Product product = product(Integer.MAX_VALUE);

            assertEquals(
                    ErrorCode.PRODUCT_STOCK_OVERFLOW,
                    assertThrows(BusinessException.class, () -> product.increaseQuantity(1)).getErrorCode()
            );
            assertEquals(Integer.MAX_VALUE, product.getQuantity());
        }

    }

    @Nested
    class DecreaseQuantity {

        @Test
        void 재고를_줄이면_수량이_빠진다() {
            Product product = product(10);

            product.decreaseQuantity(3);

            assertEquals(7, product.getQuantity());
        }

        @Test
        void 수량이_0_이하면_예외를_던지고_재고는_그대로다() {
            Product product = product(10);

            assertThrows(IllegalArgumentException.class, () -> product.decreaseQuantity(0));
            assertThrows(IllegalArgumentException.class, () -> product.decreaseQuantity(-1));
            assertEquals(10, product.getQuantity());
        }

        @Test
        void 재고와_같은_수량을_줄이면_0이_된다() {
            Product product = product(10);

            product.decreaseQuantity(10);

            assertEquals(0, product.getQuantity());
        }

        @Test
        void 재고보다_많이_줄이면_예외를_던지고_재고는_그대로다() {
            Product product = product(10);

            assertEquals(
                    ErrorCode.PRODUCT_STOCK_INSUFFICIENT,
                    assertThrows(BusinessException.class, () -> product.decreaseQuantity(11)).getErrorCode()
            );
            assertEquals(10, product.getQuantity());
        }

    }

    private Product product(int quantity) {
        Product product = new Product("WTR-500", "생수 500ml");
        product.increaseQuantity(quantity);

        return product;
    }

}
