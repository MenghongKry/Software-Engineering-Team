package edu.itc.shop.domain;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/** A transition that the order's current status does not allow: 409. */
public class OrderStateException extends ClientErrorException {
    public OrderStateException(String id, OrderStatus current, String action) {
        super("Order " + id + " is " + current + " and cannot be " + action, Response.Status.CONFLICT);
    }
}
