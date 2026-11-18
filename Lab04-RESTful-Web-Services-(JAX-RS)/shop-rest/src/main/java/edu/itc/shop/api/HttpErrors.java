package edu.itc.shop.api;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

/** Client errors that Response.Status has no constant for (Jakarta REST 4.0 has no 422). */
final class HttpErrors {

    private HttpErrors() { }

    static ClientErrorException unprocessable(String detail) {
        return new ClientErrorException(detail, Response.status(422, "Unprocessable Content").build());
    }

    static ClientErrorException preconditionRequired(String detail) {
        return new ClientErrorException(detail, Response.Status.PRECONDITION_REQUIRED);
    }
}
