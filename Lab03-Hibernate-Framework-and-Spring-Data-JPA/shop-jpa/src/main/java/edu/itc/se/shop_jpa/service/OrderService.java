package edu.itc.se.shop_jpa.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderDto;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) { this.orders = orders; }

    @Transactional(readOnly = true)
    public Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable) {
        return orders.findByStatus(status, pageable);
    }

    /** Fetch join loads orders + lines + products in one query; mapping happens inside the tx. */
    @Transactional(readOnly = true)
    public List<OrderDto> ordersOfCustomer(String email) {
        return orders.findWithLinesByCustomerEmail(email).stream()
                .map(OrderDto::from)
                .toList();
    }
}