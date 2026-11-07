package edu.itc.se.shop_jpa.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.service.OrderService;
import edu.itc.se.shop_jpa.web.dto.OrderDto;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping("/orders")
    public Page<OrderSummary> list(@RequestParam OrderStatus status,
                                   @PageableDefault(size = 20, sort = "placedAt",
                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.findByStatus(status, pageable);
    }

    @GetMapping("/customers/{email}/orders")
    public List<OrderDto> ordersOfCustomer(@PathVariable String email) {
        return orderService.ordersOfCustomer(email);
    }
}