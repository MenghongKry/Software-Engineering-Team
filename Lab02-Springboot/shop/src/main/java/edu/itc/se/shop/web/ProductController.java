package edu.itc.se.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    record StockUpdate(int delta) {}

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String q) {

        return service.findAll(q).stream()
                .map(ProductDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable long id) {

        return ResponseEntity.ok(ProductDto.from(service.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProduct request) {

        Product created = service.create(request.name(), request.price(), request.stock());

        return ResponseEntity
            .created(URI.create("/api/products/" + created.id()))
            .body(ProductDto.from(created));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable long id,
            @RequestBody StockUpdate request) {

        return ResponseEntity.ok(ProductDto.from(service.updateStock(id, request.delta())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id) {

        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}