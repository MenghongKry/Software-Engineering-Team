package edu.itc.se.shop.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure money maths, no Spring, easy to unit test. */
public final class Pricing {
    private Pricing() {}

    /** net * (1 + taxRate), rounded HALF_UP to 2 decimals */
    public static BigDecimal gross(BigDecimal net, BigDecimal taxRate) {
        return net.multiply(BigDecimal.ONE.add(taxRate)).setScale(2, RoundingMode.HALF_UP);
    }
}
