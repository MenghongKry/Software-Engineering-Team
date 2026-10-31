package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.util.List;
import edu.itc.se.shop.domain.Pricing;

public final class OrderPricing {
    private OrderPricing() {}

    /** sum(unitPrice * qty) * (1 + taxRate), HALF_UP, 2 decimals */
    public static BigDecimal total(List<OrderLine> lines, BigDecimal taxRate) {
        BigDecimal net = lines.stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Pricing.gross(net, taxRate);
    }
}


