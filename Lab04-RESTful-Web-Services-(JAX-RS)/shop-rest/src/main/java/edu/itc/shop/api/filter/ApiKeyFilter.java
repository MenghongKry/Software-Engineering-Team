package edu.itc.shop.api.filter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import edu.itc.shop.api.dto.Problem;

/** Writes need a valid X-Api-Key; checked before matching. */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> keys = Arrays.stream(
                    System.getProperty("shop.api.keys", "demo-123").split(","))
            .map(String::trim)
            .filter(k -> !k.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (SAFE.contains(ctx.getMethod())) return;
        String key = ctx.getHeaderString("X-Api-Key");
        if (key != null && keys.contains(key)) return;

        var problem = new Problem("about:blank", "Unauthorized", 401,
                "Missing or invalid X-Api-Key", ctx.getUriInfo().getRequestUri().getPath(), null);
        ctx.abortWith(Response.status(401)
                .header("WWW-Authenticate", "ApiKey realm=\"shop\"")
                .type(Problem.MEDIA_TYPE)
                .entity(problem)
                .build());
    }
}
