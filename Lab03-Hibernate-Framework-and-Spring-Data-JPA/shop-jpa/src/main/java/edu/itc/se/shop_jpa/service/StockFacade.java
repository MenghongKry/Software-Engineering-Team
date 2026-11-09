package edu.itc.se.shop_jpa.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class StockFacade {

    static final int MAX_ATTEMPTS = 20;

    private final StockService stock;

    public StockFacade(StockService stock) { this.stock = stock; }

    /** @return the number of retries that were needed (0 = it worked the first time) */
    public int reserveOptimisticWithRetry(Long productId, int qty) {
        for (int attempt = 1; ; attempt++) {
            try {
                stock.reserveOptimistic(productId, qty);
                return attempt - 1;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) throw e;
                sleepQuietly(attempt);          // small back-off so the threads spread out
            }
        }
    }

    private static void sleepQuietly(int attempt) {
        try {
            Thread.sleep(2L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
