package edu.itc.shop.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;

/** Order representation with HATEOAS links in "_links" (rel -> absolute URI). */
@JsonbPropertyOrder({"id", "status", "total", "lines", "links"})
public record OrderDto(String id,
                       OrderStatus status,
                       BigDecimal total,
                       List<Line> lines,
                       @JsonbProperty("_links") Map<String, String> links) {

    public record Line(long productId, int quantity) { }

    public static OrderDto from(Order o, Map<String, String> links) {
        var lines = o.lines().stream().map(l -> new Line(l.productId(), l.quantity())).toList();
        return new OrderDto(o.id(), o.status(), o.total(), lines, links);
    }
}
