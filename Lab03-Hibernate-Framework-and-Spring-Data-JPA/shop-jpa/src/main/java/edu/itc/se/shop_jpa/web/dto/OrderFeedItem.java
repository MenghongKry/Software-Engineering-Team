package edu.itc.se.shop_jpa.web.dto;

import java.time.Instant;

import edu.itc.se.shop_jpa.domain.OrderStatus;

/** Challenge: one item of the order feed. */
public record OrderFeedItem(Long id, OrderStatus status, Instant placedAt, String customerName) { }
