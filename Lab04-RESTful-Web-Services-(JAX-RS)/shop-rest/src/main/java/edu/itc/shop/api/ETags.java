package edu.itc.shop.api;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;

import edu.itc.shop.domain.Product;

final class ETags {

    static final int ANY = -1;

    private ETags() { }

    static EntityTag of(Product p) {
        return new EntityTag(Integer.toHexString(p.version()));
    }

    static int expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw HttpErrors.preconditionRequired("If-Match header required for PUT");
        }
        String value = ifMatch.trim();
        if (value.equals("*")) return ANY;
        if (value.startsWith("W/")) value = value.substring(2);
        value = value.replace("\"", "");
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            throw new ClientErrorException("If-Match " + ifMatch + " is not a valid ETag",
                    Response.Status.PRECONDITION_FAILED);
        }
    }
}
