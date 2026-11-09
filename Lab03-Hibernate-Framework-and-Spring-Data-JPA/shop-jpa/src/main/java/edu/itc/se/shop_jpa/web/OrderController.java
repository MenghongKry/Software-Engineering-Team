package edu.itc.se.shop_jpa.web;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.service.OrderFeedService;
import edu.itc.se.shop_jpa.service.OrderService;
import edu.itc.se.shop_jpa.web.dto.OrderDto;
import edu.itc.se.shop_jpa.web.dto.OrderFeed;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final OrderFeedService feedService;

    public OrderController(OrderService orderService, OrderFeedService feedService) {
        this.orderService = orderService;
        this.feedService = feedService;
    }

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

    /** Challenge: infinite scroll with keyset paging. ?size=20&after=<placedAt>,<id> */
    @GetMapping("/orders/feed")
    public OrderFeed feed(@RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String after) {
        return feedService.keyset(after, size);
    }

    /** Challenge: the same feed with Slice (offset paging). ?size=20&page=0 */
    @GetMapping("/orders/feed/slice")
    public OrderFeed feedSlice(@RequestParam(defaultValue = "20") int size,
                               @RequestParam(defaultValue = "0") int page) {
        return feedService.slice(page, size);
    }
}
