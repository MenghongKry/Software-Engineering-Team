package edu.itc.se.shop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(long id, long customerId, OrderStatus status, LocalDateTime placedAt, BigDecimal total) {}
