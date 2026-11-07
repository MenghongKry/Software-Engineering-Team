package edu.itc.se.shop_jpa.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;

public final class ProductSpecs {
    private ProductSpecs() { }

    /** lower(name) like %q% */
    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.<String>get("name")), "%" + q.toLowerCase() + "%");
    }

    /** category = ? */
    public static Specification<Product> inCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** amount <= ?   (path into the embedded Money) */
    public static Specification<Product> cheaperThan(BigDecimal max) {
        return (root, query, cb) ->
                cb.le(root.<Money>get("price").<BigDecimal>get("amount"), max);
    }

    /** stock > 0 */
    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.<Integer>get("stock"), 0);
    }

    /** Combines only the filters whose parameter is present. */
    public static Specification<Product> filter(String q, Category category,
                                                BigDecimal maxPrice, Boolean inStock) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (q != null && !q.isBlank())        specs.add(nameContains(q));
        if (category != null)                 specs.add(inCategory(category));
        if (maxPrice != null)                 specs.add(cheaperThan(maxPrice));
        if (Boolean.TRUE.equals(inStock))     specs.add(inStock());

        return specs.isEmpty()
                ? (root, query, cb) -> cb.conjunction()   // no filter → "where 1=1"
                : Specification.allOf(specs);
    }
}