package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

public class ProductService {

    /** Whitelist: only these fields can be used in ?sort=field,dir. */
    private static final Map<String, Comparator<Product>> SORTS = Map.of(
            "name",  Comparator.comparing(Product::name, String.CASE_INSENSITIVE_ORDER),
            "price", Comparator.comparing(Product::price),
            "stock", Comparator.comparingInt(Product::stock));

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // ---------- reads ----------

    public List<Product> search(ProductQuery q) {
        Comparator<Product> order = SORTS.get(q.sortField());
        if (order == null) {
            throw new BadRequestException(
                    "Unknown sort field '" + q.sortField() + "', use name, price or stock");
        }
        if (q.descending()) order = order.reversed();
        requireEuro(q.minPrice(), "minPrice");
        requireEuro(q.maxPrice(), "maxPrice");

        String text = q.text() == null ? null : q.text().toLowerCase(Locale.ROOT);
        return repo.findAll().stream()
                .filter(p -> text == null || text.isBlank()
                        || p.name().toLowerCase(Locale.ROOT).contains(text))
                .filter(p -> q.minPrice() == null || p.price().compareTo(q.minPrice().amount()) >= 0)
                .filter(p -> q.maxPrice() == null || p.price().compareTo(q.maxPrice().amount()) <= 0)
                .filter(p -> !q.inStockOnly() || p.stock() > 0)
                .sorted(order.thenComparingLong(Product::id))
                .toList();
    }

    /** Task 5: filter + sort, then cut out one page. */
    public Page<Product> searchPage(ProductQuery q, int page, int size) {
        List<Product> all = search(q);
        List<Product> items = all.stream().skip((long) page * size).limit(size).toList();
        return new Page<>(items, page, size, all.size());
    }

    public Product get(long id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    // ---------- writes ----------

    public synchronized Product create(Product data) {
        repo.findBySku(data.sku()).ifPresent(p -> { throw new DuplicateSkuException(data.sku()); });
        return repo.save(new Product(0, data.name(), data.sku(), data.price(), data.stock(), 0));
    }

    /**
     * Task 5: optimistic locking. {@code expectedVersion} comes from If-Match;
     * -1 means "If-Match: *" (any current version). Check and save happen in one
     * synchronized block, so two clients with the same ETag cannot both win.
     */
    public synchronized Product replace(long id, Product data, int expectedVersion) {
        Product existing = get(id);
        if (expectedVersion != -1 && expectedVersion != existing.version()) {
            throw new StaleVersionException(id, expectedVersion, existing.version());
        }
        return replace(id, data);
    }

    public synchronized Product replace(long id, Product data) {
        Product existing = get(id);
        repo.findBySku(data.sku())
                .filter(other -> other.id() != id)
                .ifPresent(other -> { throw new DuplicateSkuException(data.sku()); });
        return repo.save(new Product(id, data.name(), data.sku(), data.price(), data.stock(),
                existing.version()));
    }

    public synchronized Product changeStock(long id, int delta) {
        Product p = get(id);
        int newStock = p.stock() + delta;
        if (newStock < 0) throw new InsufficientStockException(p.stock(), delta);
        return repo.save(new Product(p.id(), p.name(), p.sku(), p.price(), newStock, p.version()));
    }

    /**
     * Challenge: takes the stock for all lines of an order at once, or for none of them.
     * Unknown product: 404, not enough stock: 409. Returns the order total.
     */
    public synchronized BigDecimal reserve(List<OrderLine> lines) {
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (OrderLine line : lines) wanted.merge(line.productId(), line.quantity(), Integer::sum);

        // 1) check everything first, so a failure changes nothing
        BigDecimal total = BigDecimal.ZERO;
        for (var e : wanted.entrySet()) {
            Product p = get(e.getKey());
            if (p.stock() < e.getValue()) throw new InsufficientStockException(p.stock(), -e.getValue());
            total = total.add(p.price().multiply(BigDecimal.valueOf(e.getValue())));
        }
        // 2) then take the stock
        for (var e : wanted.entrySet()) changeStock(e.getKey(), -e.getValue());
        return total;
    }

    /** Challenge: gives the stock of a cancelled order back (skips products deleted since). */
    public synchronized void release(List<OrderLine> lines) {
        for (OrderLine line : lines) {
            repo.findById(line.productId()).ifPresent(p -> changeStock(p.id(), line.quantity()));
        }
    }

    public synchronized void delete(long id) {
        get(id);                 // 404 when missing
        repo.delete(id);
    }

    private static void requireEuro(Money m, String param) {
        if (m != null && !m.inCurrency("EUR")) {
            throw new BadRequestException(param + " must be in EUR, got " + m.currency());
        }
    }
}