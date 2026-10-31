package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public long insert(long customerId, OrderStatus status, LocalDateTime placedAt,
                       BigDecimal total, List<OrderLine> lines) {
        var keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO orders(customer_id, status, placed_at, total) VALUES (:c, :s, :p, :t)")
                .param("c", customerId)
                .param("s", status.name())
                .param("p", placedAt)
                .param("t", total)
                .update(keys, "id");
        long id = keys.getKey().longValue();
        for (OrderLine l : lines) {
            jdbc.sql("INSERT INTO order_line(order_id, product_id, qty, unit_price) VALUES (:o, :p, :q, :u)")
                    .param("o", id).param("p", l.productId()).param("q", l.qty()).param("u", l.unitPrice())
                    .update();
        }
        return id;
    }

    public Optional<Order> findById(long id) {
        return jdbc.sql("SELECT id, customer_id, status, placed_at, total FROM orders WHERE id = :id")
                .param("id", id)
                .query(Order.class).optional();
    }

    public List<OrderLine> findLines(long orderId) {
        return findLinesFor(List.of(orderId));
    }

    public List<OrderLine> findLinesFor(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) return List.of();
        return jdbc.sql("""
                SELECT order_id, product_id, qty, unit_price FROM order_line
                WHERE order_id IN (:ids) ORDER BY order_id, product_id""")
                .param("ids", orderIds)
                .query(OrderLine.class).list();
    }

    public List<Order> find(Long customerId, OrderStatus status, int limit, int offset) {
        var f = filter(customerId, status);
        return jdbc.sql("SELECT id, customer_id, status, placed_at, total FROM orders" + f.sql()
                        + " ORDER BY id DESC LIMIT :limit OFFSET :offset")
                .params(f.params())
                .param("limit", limit)
                .param("offset", offset)
                .query(Order.class).list();
    }

    public long count(Long customerId, OrderStatus status) {
        var f = filter(customerId, status);
        return jdbc.sql("SELECT COUNT(*) FROM orders" + f.sql())
                .params(f.params())
                .query(Long.class).single();
    }

    /** Only changes the row if it still has the expected status. Returns affected rows. */
    public int updateStatus(long id, OrderStatus expected, OrderStatus next) {
        return jdbc.sql("UPDATE orders SET status = :next WHERE id = :id AND status = :expected")
                .param("next", next.name())
                .param("id", id)
                .param("expected", expected.name())
                .update();
    }

    // only fixed SQL fragments are concatenated; every value is a bound parameter
    private record Filter(String sql, Map<String, Object> params) {}

    private static Filter filter(Long customerId, OrderStatus status) {
        var sql = new StringBuilder(" WHERE 1 = 1");
        var params = new HashMap<String, Object>();
        if (customerId != null) { sql.append(" AND customer_id = :customerId"); params.put("customerId", customerId); }
        if (status != null)     { sql.append(" AND status = :status");          params.put("status", status.name()); }
        return new Filter(sql.toString(), params);
    }
}
