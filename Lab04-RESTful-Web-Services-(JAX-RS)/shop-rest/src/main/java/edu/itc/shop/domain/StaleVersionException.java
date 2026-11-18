package edu.itc.shop.domain;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/** The client edited an old version (If-Match does not match): 412. */
public class StaleVersionException extends ClientErrorException {
    public StaleVersionException(long id, int expected, int current) {
        super("Product " + id + " is at version " + current + ", If-Match was for version " + expected,
                Response.Status.PRECONDITION_FAILED);
    }
}
