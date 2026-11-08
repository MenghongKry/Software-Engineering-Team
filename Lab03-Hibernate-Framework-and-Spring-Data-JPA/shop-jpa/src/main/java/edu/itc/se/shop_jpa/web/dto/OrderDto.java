package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;

public record OrderDto(Long id, OrderStatus status, Instant placedAt,
                       BigDecimal total, List<OrderLineDto> lines) {
    public static OrderDto from(Order o) {
        return new OrderDto(o.getId(), o.getStatus(), o.getPlacedAt(), o.total().amount(),
                o.getLines().stream().map(OrderLineDto::from).toList());
    }
}