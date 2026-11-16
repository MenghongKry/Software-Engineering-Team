package edu.itc.shop.domain;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class InsufficientStockException extends ClientErrorException {
    public InsufficientStockException(int stock, int delta) {
        super("Insufficient stock: have " + stock + ", change " + delta,
                Response.Status.CONFLICT);
    }
}