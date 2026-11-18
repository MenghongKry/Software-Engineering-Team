package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Order rules: NEW -> PAID, NEW -> CANCELLED (stock goes back). Everything else is 409.
 * One shared instance; the map is concurrent and every state change is synchronized.
 */
public class OrderService {

    private final ProductService products;
    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1000);

    public OrderService(ProductService products) {
        this.products = products;
    }

    /** Reserves the stock of every line (404 / 409 inside) and creates a NEW order. */
    public Order place(List<OrderLine> lines) {
        BigDecimal total = products.reserve(lines);
        Order order = new Order("o-" + nextId.incrementAndGet(), lines, total, OrderStatus.NEW);
        orders.put(order.id(), order);
        return order;
    }

    public Order get(String id) {
        Order order = orders.get(id);
        if (order == null) throw new OrderNotFoundException(id);
        return order;
    }

    public synchronized Order pay(String id) {
        Order order = get(id);
        if (order.status() != OrderStatus.NEW) throw new OrderStateException(id, order.status(), "paid");
        return save(order.withStatus(OrderStatus.PAID));
    }

    public synchronized Order cancel(String id) {
        Order order = get(id);
        if (order.status() != OrderStatus.NEW) throw new OrderStateException(id, order.status(), "cancelled");
        products.release(order.lines());
        return save(order.withStatus(OrderStatus.CANCELLED));
    }

    private Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }
}
