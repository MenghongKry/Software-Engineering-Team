package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(long id, long customerId, OrderStatus status, LocalDateTime placedAt,
                       BigDecimal total, List<LineDto> lines) {

    public record LineDto(long productId, int qty, BigDecimal unitPrice) {}

    static OrderDto of(Order o, List<OrderLine> lines) {
        return new OrderDto(o.id(), o.customerId(), o.status(), o.placedAt(), o.total(),
                lines.stream().map(l -> new LineDto(l.productId(), l.qty(), l.unitPrice())).toList());
    }
}
