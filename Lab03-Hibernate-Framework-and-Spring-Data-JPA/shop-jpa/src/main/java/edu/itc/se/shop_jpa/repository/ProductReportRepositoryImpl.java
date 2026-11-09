package edu.itc.se.shop_jpa.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.itc.se.shop_jpa.domain.Money_;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Product_;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.domain.Tag_;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ProductReportRepositoryImpl implements ProductReportRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProductReportRow> report(ProductReportFilter f) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ProductReportRow> cq = cb.createQuery(ProductReportRow.class);
        Root<Product> p = cq.from(Product.class);

        Expression<Integer> tagCount = cb.size(p.get(Product_.tags));
        Expression<BigDecimal> amount = p.get(Product_.price).get(Money_.amount);

        List<Predicate> where = new ArrayList<>();
        if (f.q() != null && !f.q().isBlank()) {
            where.add(cb.like(cb.lower(p.get(Product_.name)), "%" + f.q().toLowerCase(Locale.ROOT) + "%"));
        }
        if (f.category() != null) {
            where.add(cb.equal(p.get(Product_.category), f.category()));
        }
        if (f.maxPrice() != null) {
            where.add(cb.le(amount, f.maxPrice()));
        }
        if (f.minStock() != null) {
            where.add(cb.ge(p.get(Product_.stock), f.minStock()));
        }
        if (f.tag() != null && !f.tag().isBlank()) {
            // the only join to tags, and only when it is needed
            Join<Product, Tag> filter = p.join(Product_.tags);
            where.add(cb.equal(filter.get(Tag_.name), f.tag()));
        }

        cq.select(cb.construct(ProductReportRow.class,
                        p.get(Product_.sku), p.get(Product_.name), amount, p.get(Product_.stock), tagCount))
          .where(where.toArray(Predicate[]::new))
          .groupBy(p.get(Product_.id), p.get(Product_.sku), p.get(Product_.name), amount, p.get(Product_.stock))
          .orderBy(order(cb, f.sort(), Map.of(
                  "sku", p.get(Product_.sku),
                  "name", p.get(Product_.name),
                  "amount", amount,
                  "stock", p.get(Product_.stock),
                  "tagCount", tagCount)));

        return em.createQuery(cq).getResultList();
    }

    /** "amount,desc" → ORDER BY amount DESC. Unknown columns are rejected (whitelist). */
    private static jakarta.persistence.criteria.Order order(CriteriaBuilder cb, String sort,
            Map<String, Expression<?>> sortable) {
        String property = "sku";
        boolean desc = false;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            property = parts[0].trim();
            desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        }
        Expression<?> column = sortable.get(property);
        if (column == null) {
            throw new IllegalArgumentException(
                    "Cannot sort by '" + property + "'. Allowed: " + sortable.keySet());
        }
        return desc ? cb.desc(column) : cb.asc(column);
    }
}
