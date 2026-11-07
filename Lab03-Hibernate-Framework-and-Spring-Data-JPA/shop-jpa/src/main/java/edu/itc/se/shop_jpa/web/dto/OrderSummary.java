package edu.itc.se.shop_jpa.web.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.itc.se.shop_jpa.domain.OrderStatus;

public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    Instant getPlacedAt();

    @JsonIgnore
    String getCustomerName();

    default CustomerName getCustomer() { return new CustomerName(getCustomerName()); }

    record CustomerName(String name) { }
}