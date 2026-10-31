package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        BigDecimal actual = OrderPricing.total(
            lines,
            new BigDecimal("0.10")
        );

        assertEquals(
            new BigDecimal("120.78"),
            actual
        );
    }
}