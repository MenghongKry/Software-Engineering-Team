package edu.itc.se.shop.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderPricingTest {

    @Test
    void calculatesTaxAndRoundsHalfUp() {
        var lines = List.of(
            new OrderLine(
                0, 1, 2, new BigDecimal("49.90")
            ),
            new OrderLine(
                0, 3, 1, new BigDecimal("10.00")
            )
        );

        assertEquals(
            new BigDecimal("120.78"),
            OrderPricing.total(
                lines,
                new BigDecimal("0.10")
            )
        );
    }
}