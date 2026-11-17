package edu.itc.shop.domain;

import jakarta.ws.rs.NotFoundException;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(long id) {
        super("Product " + id + " not found");
    }
}