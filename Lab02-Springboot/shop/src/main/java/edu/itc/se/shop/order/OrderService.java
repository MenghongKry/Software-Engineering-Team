package edu.itc.se.shop.order;


import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import static java.util.Comparator.comparingLong;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.stream.Collectors.groupingBy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.config.ShopProperties;
import edu.itc.se.shop.domain.Page;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.CustomerRepository;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.service.InsufficientStockException;
import edu.itc.se.shop.service.Notifier;
import edu.itc.se.shop.service.ProductNotFoundException;


@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final ShopProperties props;
    private final Notifier notifier;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, CustomerRepository customers,
                        ShopProperties props, Notifier notifier, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.customers = customers;
        this.props = props;
        this.notifier = notifier;
        this.clock = clock;
    }

    // ---------- 2. Placing an order ----------
    @Audited
    @Transactional
    public OrderDto place(PlaceOrder cmd) {
        // 1. no duplicate productId
        Set<Long> seen = new HashSet<>();
        for (var l : cmd.lines()) {
            if (!seen.add(l.productId())) {
                throw new OrderExceptions.InvalidOrder("Duplicate product " + l.productId() + " in order");
            }
        }
        if (!customers.exists(cmd.customerId())) {
            throw new OrderExceptions.CustomerNotFound(cmd.customerId());
        }

        // 2. decrement stock for every line (sorted by id so concurrent orders lock rows in the same order)
        List<OrderLine> lines = new ArrayList<>();
        var sorted = cmd.lines().stream().sorted(comparingLong(PlaceOrder.Line::productId)).toList();
        for (var l : sorted) {
            Product p = products.findById(l.productId())
                    .orElseThrow(() -> new ProductNotFoundException(l.productId()));
            if (products.updateStock(p.id(), -l.qty()) == 0) {
                throw new InsufficientStockException("Insufficient stock for product " + p.id());
            }
            lines.add(new OrderLine(0, p.id(), l.qty(), p.price()));   // 3. price snapshot
        }

        // 3. total with tax, HALF_UP
        var total = OrderPricing.total(lines, props.taxRate());

        // 4. persist, 5. return
        long id = orders.insert(cmd.customerId(), OrderStatus.NEW, LocalDateTime.now(clock), total, lines);
        return load(id);
    }

    // ---------- 3. Reading ----------
    @Transactional(readOnly = true)
    public OrderDto get(long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> list(Long customerId, OrderStatus status, int page, int size) {
        int s = Math.min(Math.max(size, 1), 100);
        int p = Math.max(page, 0);
        List<Order> found = orders.find(customerId, status, s, p * s);
        Map<Long, List<OrderLine>> linesByOrder = orders.findLinesFor(found.stream().map(Order::id).toList())
                .stream().collect(groupingBy(OrderLine::orderId));
        var content = found.stream()
                .map(o -> OrderDto.of(o, linesByOrder.getOrDefault(o.id(), List.of())))
                .toList();
        return new Page<>(content, p, s, orders.count(customerId, status));
    }

    // ---------- 4. Cancelling ----------
    @Audited
    @Transactional
    public OrderDto cancel(long id) {
        Order order = orders.findById(id).orElseThrow(() -> new OrderExceptions.NotFound(id));

        // only a NEW order can become CANCELLED, even if two cancels race
        if (orders.updateStatus(id, OrderStatus.NEW, OrderStatus.CANCELLED) == 0) {
            throw new OrderExceptions.InvalidState(
                    "Order " + id + " is " + order.status() + ", only NEW orders can be cancelled");
        }
        for (OrderLine l : orders.findLines(id)) {
            products.updateStock(l.productId(), l.qty());              // restore stock
        }
        notifier.send("customer-" + order.customerId(), "Order " + id + " was cancelled");
        return load(id);
    }

    private OrderDto load(long id) {
        Order o = orders.findById(id).orElseThrow(() -> new OrderExceptions.NotFound(id));
        return OrderDto.of(o, orders.findLines(id));
    }
}
