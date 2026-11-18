package edu.itc.shop.api.filter;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/** Correlation id + one access-log line per request. */
@Provider
@Priority(Priorities.USER)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final String ID_PROPERTY = "shop.correlationId";
    private static final String START_PROPERTY = "shop.startNanos";
    private static final Logger LOG = System.getLogger("access");

    @Override
    public void filter(ContainerRequestContext request) {
        request.setProperty(ID_PROPERTY, idOf(request));
        request.setProperty(START_PROPERTY, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // request side is skipped when a pre-matching filter aborts
        Object id = request.getProperty(ID_PROPERTY);
        if (id == null) id = idOf(request);
        Object start = request.getProperty(START_PROPERTY);
        long ms = start == null ? 0 : (System.nanoTime() - (Long) start) / 1_000_000;

        response.getHeaders().putSingle(HEADER, id);
        LOG.log(Level.INFO, request.getMethod() + " " + request.getUriInfo().getRequestUri().getPath()
                + " -> " + response.getStatus() + " [" + id + "] " + ms + " ms");
    }

    private static String idOf(ContainerRequestContext request) {
        String id = request.getHeaderString(HEADER);
        return (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    }
}
