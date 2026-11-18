package edu.itc.shop.domain;

import jakarta.ws.rs.NotFoundException;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(String id) {
        super("Order " + id + " not found");
    }
}
