package edu.itc.se.shop_jpa.web.dto;

import java.util.List;

/** Challenge: answer of GET /api/orders/feed → { items, nextCursor, hasNext }. */
public record OrderFeed(List<OrderFeedItem> items, String nextCursor, boolean hasNext) { }
