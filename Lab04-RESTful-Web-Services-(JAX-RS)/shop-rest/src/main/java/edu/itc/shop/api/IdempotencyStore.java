package edu.itc.shop.api;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import edu.itc.shop.api.dto.OrderDto;

/**
 * Remembers the first response for each Idempotency-Key (one shared instance, bound in Main).
 * computeIfAbsent runs the supplier at most once per key, even for two retries arriving at the
 * same time; the second caller waits and then gets the stored response.
 */
public class IdempotencyStore {

    public record Stored(String bodyHash, int status, URI location, OrderDto body) { }

    /** What a POST gets back: the stored response and whether it was a replay. */
    public record Result(Stored stored, boolean replayed) { }

    private final ConcurrentHashMap<String, Stored> responses = new ConcurrentHashMap<>();

    public Result once(String key, Supplier<Stored> firstCall) {
        boolean[] created = { false };
        Stored stored = responses.computeIfAbsent(key, k -> {
            created[0] = true;
            return firstCall.get();
        });
        return new Result(stored, !created[0]);
    }
}
