package edu.itc.se.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    /** Moves qty units of stock from one product to another: both updates happen, or neither. */
    @Transactional
    public void restock(long fromId, long toId, int qty) {
        if (repo.updateStock(fromId, -qty) == 0) {
            throw new IllegalStateException("not enough stock in product " + fromId);
        }
        if (repo.updateStock(toId, qty) == 0) {
            throw new IllegalStateException("product " + toId + " not found");
        }
    }
}
