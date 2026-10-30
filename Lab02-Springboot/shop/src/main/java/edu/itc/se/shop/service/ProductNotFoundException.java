package edu.itc.se.shop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends IllegalStateException {
    public ProductNotFoundException(long productId) {
        super("product " + productId + " not found");
    }
}
