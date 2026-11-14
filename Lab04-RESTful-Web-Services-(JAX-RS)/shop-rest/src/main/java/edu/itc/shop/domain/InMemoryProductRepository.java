package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<Long, Product> products =
            new ConcurrentHashMap<>();

    private final AtomicLong nextId = new AtomicLong(0);

    public InMemoryProductRepository() {
        save(new Product(0, "Kettle", "KT-01",
                new BigDecimal("24.90"), 12, 0));

        save(new Product(0, "Mug", "MG-01",
                new BigDecimal("6.50"), 40, 0));

        save(new Product(0, "Tea", "TE-01",
                new BigDecimal("4.20"), 0, 0));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return products.values().stream()
                .filter(p -> p.sku().equals(sku))
                .findFirst();
    }

    @Override
    public Product save(Product product) {

        long id = product.id();

        if (id == 0) {
            id = nextId.incrementAndGet();
        }

        long finalId = id;

        return products.compute(finalId, (key, existing) -> {

            int version = (existing == null)
                    ? 1
                    : existing.version() + 1;

            return new Product(
                    finalId,
                    product.name(),
                    product.sku(),
                    product.price(),
                    product.stock(),
                    version
            );
        });
    }

    @Override
    public void delete(long id) {
        products.remove(id);
    }
}
