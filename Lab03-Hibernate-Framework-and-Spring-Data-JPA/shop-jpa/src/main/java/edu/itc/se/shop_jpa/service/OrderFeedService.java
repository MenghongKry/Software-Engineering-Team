package edu.itc.se.shop_jpa.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderFeed;
import edu.itc.se.shop_jpa.web.dto.OrderFeedItem;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

@Service
public class OrderFeedService {

    private final OrderRepository orders;

    public OrderFeedService(OrderRepository orders) { this.orders = orders; }

    @Transactional(readOnly = true)
    public OrderFeed keyset(String after, int size) {
        ScrollPosition position = after == null || after.isBlank()
                ? ScrollPosition.keyset()                           // first page
                : ScrollPosition.forward(decode(after));
        Window<Order> window = orders.findAllByOrderByPlacedAtDescIdDesc(position, Limit.of(size));

        List<OrderFeedItem> items = window.getContent().stream()
                .map(o -> new OrderFeedItem(o.getId(), o.getStatus(), o.getPlacedAt(), o.getCustomer().getName()))
                .toList();
        String next = window.hasNext() && !items.isEmpty() ? encode(items.getLast()) : null;
        return new OrderFeed(items, next, window.hasNext());
    }

    @Transactional(readOnly = true)
    public OrderFeed slice(int page, int size) {
        Slice<OrderSummary> slice = orders.feedSlice(PageRequest.of(page, size));
        List<OrderFeedItem> items = slice.getContent().stream()
                .map(s -> new OrderFeedItem(s.getId(), s.getStatus(), s.getPlacedAt(), s.getCustomerName()))
                .toList();
        return new OrderFeed(items, slice.hasNext() ? String.valueOf(page + 1) : null, slice.hasNext());
    }

    private static String encode(OrderFeedItem last) {
        return last.placedAt() + "," + last.id();
    }

    private static Map<String, Object> decode(String cursor) {
        String[] parts = cursor.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cursor must look like <placedAt>,<id>");
        }
        return Map.of("placedAt", Instant.parse(parts[0].trim()), "id", Long.valueOf(parts[1].trim()));
    }
}
