package com.github.s8u.inventory.usecase;

import com.github.s8u.inventory.TestcontainersConfiguration;
import com.github.s8u.inventory.entity.Product;
import com.github.s8u.inventory.entity.ProductStockHistory;
import com.github.s8u.inventory.enums.ProductStockHistoryType;
import com.github.s8u.inventory.exception.BusinessException;
import com.github.s8u.inventory.exception.ErrorCode;
import com.github.s8u.inventory.repository.ProductRepository;
import com.github.s8u.inventory.repository.ProductStockHistoryRepository;
import com.github.s8u.inventory.usecase.dto.command.ProductInboundCommand;
import com.github.s8u.inventory.usecase.dto.command.ProductOutboundCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 입고, 출고 동시성 통합 테스트
 *
 * 행 락 대기, UNIQUE 충돌 처리처럼 DB 엔진 동작을 확인하므로 실제 PostgreSQL(Testcontainers)로 돌린다.
 * 테스트 메서드에 @Transactional을 걸지 않는다. 각 스레드가 준비한 데이터를 커밋된 상태로 봐야 한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ProductStockConcurrencyTest {

    private static final String SKU = "WTR-500";

    @Autowired
    private ProductInboundUseCase productInboundUseCase;

    @Autowired
    private ProductOutboundUseCase productOutboundUseCase;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductStockHistoryRepository productStockHistoryRepository;

    @BeforeEach
    void setUp() {
        productStockHistoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Nested
    class InboundProduct {

        @Test
        void 같은_상품을_동시에_입고하면_모두_재고에_반영된다() throws InterruptedException {
            saveProduct(0);

            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++)
                tasks.add(() -> productInboundUseCase.inbound(new ProductInboundCommand(SKU, null, 1)));

            List<Throwable> failures = runConcurrently(tasks);

            assertEquals(List.of(), failures);
            assertEquals(100, productRepository.findBySku(SKU).orElseThrow().getQuantity());
            assertEquals(Map.of(ProductStockHistoryType.INBOUND, 100L), countHistoriesByType());
            assertHistoriesConnected(0);
        }

        /**
         * 같은 새 SKU를 두 요청이 동시에 입고한다.
         *
         * 한 요청이 등록을 커밋한 뒤에 다른 요청이 조회하면 이미 있는 상품 입고와 같아져 등록 충돌이 나지 않는다.
         * 한 쌍으로는 겹치지 않을 수 있어 SKU 50개로 여러 번 시도한다.
         */
        @Test
        void 새_SKU를_동시에_입고하면_모두_성공한다() throws InterruptedException {
            List<String> skus = IntStream.rangeClosed(1, 50).mapToObj("NEW-%03d"::formatted).toList();

            List<Runnable> tasks = new ArrayList<>();
            for (String sku : skus) {
                tasks.add(() -> productInboundUseCase.inbound(new ProductInboundCommand(sku, "상품 " + sku, 5)));
                tasks.add(() -> productInboundUseCase.inbound(new ProductInboundCommand(sku, "상품 " + sku, 5)));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertEquals(List.of(), failures);
            assertEquals(50, productRepository.count());
            for (String sku : skus)
                assertEquals(10, productRepository.findBySku(sku).orElseThrow().getQuantity());
            assertEquals(Map.of(ProductStockHistoryType.INBOUND, 100L), countHistoriesByType());
        }

    }

    @Nested
    class OutboundProduct {

        @Test
        void 같은_상품을_동시에_출고하면_모두_재고에_반영된다() throws InterruptedException {
            saveProduct(100);

            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++)
                tasks.add(() -> productOutboundUseCase.outbound(new ProductOutboundCommand(SKU, 1)));

            List<Throwable> failures = runConcurrently(tasks);

            assertEquals(List.of(), failures);
            assertEquals(0, productRepository.findBySku(SKU).orElseThrow().getQuantity());
            assertEquals(Map.of(ProductStockHistoryType.OUTBOUND, 100L), countHistoriesByType());
            assertHistoriesConnected(100);
        }

        @Test
        void 재고보다_많이_동시에_출고하면_재고만큼만_성공한다() throws InterruptedException {
            saveProduct(50);

            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++)
                tasks.add(() -> productOutboundUseCase.outbound(new ProductOutboundCommand(SKU, 1)));

            List<Throwable> failures = runConcurrently(tasks);

            assertEquals(50, failures.size());
            assertTrue(failures.stream().allMatch(failure -> isErrorCode(failure, ErrorCode.PRODUCT_STOCK_INSUFFICIENT)));
            assertEquals(0, productRepository.findBySku(SKU).orElseThrow().getQuantity());
            assertEquals(Map.of(ProductStockHistoryType.OUTBOUND, 50L), countHistoriesByType());
            assertHistoriesConnected(50);
        }

    }

    @Nested
    class InboundAndOutboundProduct {

        @Test
        void 같은_상품을_동시에_입고하고_출고하면_모두_재고에_반영된다() throws InterruptedException {
            saveProduct(100);

            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> productInboundUseCase.inbound(new ProductInboundCommand(SKU, null, 1)));
                tasks.add(() -> productOutboundUseCase.outbound(new ProductOutboundCommand(SKU, 1)));
            }

            List<Throwable> failures = runConcurrently(tasks);

            assertEquals(List.of(), failures);
            assertEquals(100, productRepository.findBySku(SKU).orElseThrow().getQuantity());
            assertEquals(
                    Map.of(ProductStockHistoryType.INBOUND, 50L, ProductStockHistoryType.OUTBOUND, 50L),
                    countHistoriesByType()
            );
            assertHistoriesConnected(100);
        }

    }

    private void saveProduct(int quantity) {
        Product product = new Product(SKU, "생수 500ml");
        if (quantity > 0)
            product.increaseQuantity(quantity);
        productRepository.save(product);
    }

    /**
     * 작업을 스레드 하나씩에 맡겨 동시에 출발시키고, 실패한 작업의 예외를 모아 돌려준다.
     *
     * 모든 스레드가 준비될 때까지 기다린 뒤 출발 신호를 한 번에 줘서 요청이 최대한 겹치게 한다.
     */
    private List<Throwable> runConcurrently(List<Runnable> tasks) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        for (Runnable task : tasks) {
            executor.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run();
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "30초 안에 모든 요청이 끝나지 않았다");

        return failures;
    }

    private boolean isErrorCode(Throwable failure, ErrorCode errorCode) {
        return failure instanceof BusinessException businessException && businessException.getErrorCode() == errorCode;
    }

    /**
     * 이력을 순서대로 읽어 각 이력의 변동 전 수량이 앞 이력의 변동 후 수량과 같은지 확인한다.
     *
     * 락 없이 두 요청이 같은 재고를 읽고 덮어쓰면 이 연결이 끊긴다.
     * 입고와 출고를 섞으면 덮어써서 사라진 입고와 출고가 서로 상쇄돼 최종 재고만으로는 못 잡을 수 있다.
     */
    private void assertHistoriesConnected(int initialQuantity) {
        int expectedBeforeQuantity = initialQuantity;
        for (ProductStockHistory history : productStockHistoryRepository.findAll(Sort.by("id"))) {
            assertEquals(expectedBeforeQuantity, history.getBeforeQuantity(), "이력 %d의 변동 전 수량".formatted(history.getId()));
            expectedBeforeQuantity = history.getAfterQuantity();
        }
        assertEquals(expectedBeforeQuantity, productRepository.findBySku(SKU).orElseThrow().getQuantity());
    }

    private Map<ProductStockHistoryType, Long> countHistoriesByType() {
        return productStockHistoryRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProductStockHistory::getType, Collectors.counting()));
    }

}
