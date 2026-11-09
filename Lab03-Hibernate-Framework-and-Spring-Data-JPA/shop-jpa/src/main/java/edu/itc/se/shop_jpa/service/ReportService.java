package edu.itc.se.shop_jpa.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderLine;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ReportService {

    public static final int BATCH_SIZE = 16;

    private final OrderRepository orders;

    @PersistenceContext
    private EntityManager em;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }


    // SELECT for the orders + 1 SELECT per order for its lines = N + 1
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrder() {
        return linesPerOrder(orders.findByStatus(OrderStatus.PAID));
    }

    // join fetch
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderJoinFetch() {
        return linesPerOrder(orders.findByStatusWithLines(OrderStatus.PAID));
    }

    // @EntityGraph
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderEntityGraph() {
        return linesPerOrder(orders.findWithGraphByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderBatched() {
        enableBatchFetching();
        return linesPerOrder(orders.findByStatus(OrderStatus.PAID));
    }

    // ---------------- product names per order (the "regression" experiment) ----------------

    @Transactional(readOnly = true)
    public Map<Long, List<String>> productNamesPerPaidOrder() {
        return productNamesPerOrder(orders.findByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> productNamesPerPaidOrderJoinFetch() {
        return productNamesPerOrder(orders.findByStatusWithLines(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> productNamesPerPaidOrderEntityGraph() {
        return productNamesPerOrder(orders.findWithGraphByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> productNamesPerPaidOrderBatched() {
        enableBatchFetching();
        return productNamesPerOrder(orders.findByStatus(OrderStatus.PAID));
    }

   
    private void enableBatchFetching() {
        em.unwrap(Session.class).setFetchBatchSize(BATCH_SIZE);
    }

    private static Map<Long, Integer> linesPerOrder(List<Order> list) {
        return collect(list, o -> o.getLines().size());
    }

    private static Map<Long, List<String>> productNamesPerOrder(List<Order> list) {
        return collect(list, o -> o.getLines().stream()
                .map(OrderLine::getProduct)
                .map(p -> p.getName())
                .toList());
    }

    private static <V> Map<Long, V> collect(List<Order> list, Function<Order, V> value) {
        Map<Long, V> result = new LinkedHashMap<>();
        for (Order o : list) {
            result.put(o.getId(), value.apply(o));
        }
        return result;
    }
}
