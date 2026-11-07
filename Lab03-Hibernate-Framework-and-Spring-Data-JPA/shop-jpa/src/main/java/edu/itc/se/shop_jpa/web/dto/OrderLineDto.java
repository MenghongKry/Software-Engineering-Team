package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;
import edu.itc.se.shop_jpa.domain.OrderLine;

public record OrderLineDto(String sku, String productName, int quantity, BigDecimal unitPrice) {
    public static OrderLineDto from(OrderLine l) {
        return new OrderLineDto(l.getProduct().getSku(), l.getProduct().getName(),
                l.getQuantity(), l.getUnitPrice().amount());
    }
}