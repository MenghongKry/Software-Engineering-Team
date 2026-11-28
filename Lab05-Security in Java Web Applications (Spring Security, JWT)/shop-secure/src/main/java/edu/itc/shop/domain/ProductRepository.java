
package edu.itc.shop.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final Map<Long, Product> products =
            new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product save(Product product) {

        long id = product.id() == 0
                ? idGenerator.incrementAndGet()
                : product.id();

        idGenerator.accumulateAndGet(id, Math::max);

        Product saved = new Product(
                id,
                product.name(),
                product.price(),
                product.stock()
        );

        products.put(id, saved);

        return saved;
    }

    public void deleteById(long id) {
        products.remove(id);
    }
}
