package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.service.OutOfStockException;
import edu.itc.se.shop_jpa.service.StockFacade;
import edu.itc.se.shop_jpa.service.StockService;

/**
 * Challenge: 10 threads each try to reserve 1 unit of a product that has only 5.
 * Exactly 5 must succeed, 5 must get OutOfStockException, and the stock must end at 0.
 */
@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=INFO",
        "logging.level.org.hibernate.orm.jdbc.bind=INFO"
})
class StockReservationTest {

    static final Logger log = LoggerFactory.getLogger(StockReservationTest.class);
    static final int THREADS = 10;
    static final int STOCK = 5;

    @Autowired ProductRepository products;
    @Autowired StockService stock;
    @Autowired StockFacade facade;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() { jdbc.update("delete from product where sku like 'STK-%'"); }

    @Test
    void pessimisticLockNeverOversells() throws Exception {
        Result r = race("STK-PES", id -> { stock.reserve(id, 1); return 0; });
        log.info("pessimistic: {} ok / {} out-of-stock, stock={}, {} threads in {} ms",
                r.ok, r.outOfStock, r.finalStock, THREADS, r.millis);
        r.assertNoOversell();
    }

    @Test
    void optimisticLockNeverOversellsButNeedsRetries() throws Exception {
        Result r = race("STK-OPT", id -> facade.reserveOptimisticWithRetry(id, 1));
        log.info("optimistic : {} ok / {} out-of-stock after {} retries, {} ms",
                r.ok, r.outOfStock, r.retries, r.millis);
        r.assertNoOversell();
    }

    @Test
    void atomicUpdateNeverOversells() throws Exception {
        Result r = race("STK-ATM", id -> { stock.reserveAtomic(id, 1); return 0; });
        log.info("atomic upd : {} ok / {} out-of-stock, {} ms", r.ok, r.outOfStock, r.millis);
        r.assertNoOversell();
    }

    // ---------- helpers ----------

    interface Reservation { int reserve(Long productId) throws Exception; }

    private Result race(String sku, Reservation reservation) throws Exception {
        Product p = new Product(sku, "Limited " + sku, Money.eur("10.00"), Category.TOYS);
        p.setStock(STOCK);
        Long id = products.saveAndFlush(p).getId();

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        AtomicInteger retries = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        long t0;
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        retries.addAndGet(reservation.reserve(id));
                        ok.incrementAndGet();
                    } catch (OutOfStockException e) {
                        outOfStock.incrementAndGet();
                    }
                    return null;
                }));
            }
            t0 = System.nanoTime();
            start.countDown();                          // all 10 threads go at the same moment
            for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);   // rethrows unexpected errors
        }
        long millis = (System.nanoTime() - t0) / 1_000_000;
        int finalStock = products.findById(id).orElseThrow().getStock();
        return new Result(ok.get(), outOfStock.get(), retries.get(), finalStock, millis);
    }

    record Result(int ok, int outOfStock, int retries, int finalStock, long millis) {
        void assertNoOversell() {
            assertThat(ok).isEqualTo(STOCK);
            assertThat(outOfStock).isEqualTo(THREADS - STOCK);
            assertThat(finalStock).isZero();
        }
    }
}
