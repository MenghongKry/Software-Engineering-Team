package edu.itc.se.shop_jpa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

@Service
public class StockService {

    private final ProductRepository products;

    public StockService(ProductRepository products) { this.products = products; }

    // Pessimistic: SELECT ... FOR UPDATE locks the row. Other threads wait at the SELECT until this transaction commits, so they always read the latest stock.

    @Transactional
    public void reserve(Long productId, int qty) {
        Product p = products.findForUpdate(productId).orElseThrow();
        take(p, qty);
    }
    // Optimistic: no lock. Every thread reads the stock; at commit the UPDATE checks "where version = ?". The loser gets ObjectOptimisticLockingFailureException and must retry
    @Transactional
    public void reserveOptimistic(Long productId, int qty) {
        Product p = products.findById(productId).orElseThrow();
        take(p, qty);
    }

    // Atomic: one UPDATE ... WHERE stock >= qty. No read, no lock held in Java, no retry.
    @Transactional
    public void reserveAtomic(Long productId, int qty) {
        if (products.decrementStock(productId, qty) == 0) {
            int available = products.findById(productId).map(Product::getStock).orElse(0);
            throw new OutOfStockException(productId, qty, available);
        }
    }

    private static void take(Product p, int qty) {
        if (p.getStock() < qty) {
            throw new OutOfStockException(p.getId(), qty, p.getStock());
        }
        p.setStock(p.getStock() - qty);
    }
}
