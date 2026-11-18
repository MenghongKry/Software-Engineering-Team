package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;

public record Order(String id, List<OrderLine> lines, BigDecimal total, OrderStatus status) {

    public Order {
        lines = List.copyOf(lines);
    }

    public Order withStatus(OrderStatus next) {
        return new Order(id, lines, total, next);
    }
}
