package edu.itc.se.shop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends IllegalStateException {
    public InsufficientStockException(long productId) {
        super("not enough stock in product " + productId);
    }
}
