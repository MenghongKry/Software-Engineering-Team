package edu.itc.se.shop.order;



import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.itc.se.shop.domain.Page;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderDto> place(@RequestBody @Valid PlaceOrder cmd) {
        OrderDto created = service.place(cmd);
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @GetMapping
    public Page<OrderDto> list(@RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) OrderStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return service.list(customerId, status, page, size);
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable long id) {
        return service.cancel(id);
    }
}
