package edu.itc.shop.domain;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** One shared instance (bound in Main.config()), so it is thread-safe. */
public class ReviewRepository {

    private final ConcurrentHashMap<Long, Review> reviews = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    public List<Review> findByProduct(long productId) {
        return reviews.values().stream()
                .filter(r -> r.productId() == productId)
                .sorted(Comparator.comparingLong(Review::id))
                .toList();
    }

    /** Assigns a new id and stores the review. */
    public Review add(long productId, int stars, String author, String text) {
        long id = nextId.incrementAndGet();
        Review review = new Review(id, productId, stars, author, text);
        reviews.put(id, review);
        return review;
    }
}
