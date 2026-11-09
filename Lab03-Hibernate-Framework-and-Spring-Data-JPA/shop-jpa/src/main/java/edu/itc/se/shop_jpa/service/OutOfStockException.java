package edu.itc.se.shop_jpa.service;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(Long productId, int requested, int available) {
        super("Product %d: requested %d, only %d in stock".formatted(productId, requested, available));
    }
}
