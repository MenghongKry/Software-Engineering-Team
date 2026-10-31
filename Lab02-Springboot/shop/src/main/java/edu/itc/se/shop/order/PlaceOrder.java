package edu.itc.se.shop.order;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PlaceOrder(@NotNull Long customerId,
                         @NotEmpty @Valid List<Line> lines) {
    public record Line(@NotNull Long productId, @Min(1) int qty) {}
}
