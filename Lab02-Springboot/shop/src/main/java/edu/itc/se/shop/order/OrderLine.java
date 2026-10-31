package edu.itc.se.shop.order;

import java.math.BigDecimal;

public record OrderLine(long orderId, long productId, int qty, BigDecimal unitPrice) {}
