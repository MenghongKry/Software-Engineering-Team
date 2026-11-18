package edu.itc.shop.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import edu.itc.shop.domain.OrderLine;

/** Body of POST /orders: {"lines":[{"productId":1,"quantity":2}]} */
public record PlaceOrder(@NotEmpty @Size(max = 50) List<@Valid @NotNull Line> lines) {

    public record Line(@Positive long productId, @Min(1) @Max(1000) int quantity) { }

    public List<OrderLine> toLines() {
        return lines.stream().map(l -> new OrderLine(l.productId(), l.quantity())).toList();
    }
}
