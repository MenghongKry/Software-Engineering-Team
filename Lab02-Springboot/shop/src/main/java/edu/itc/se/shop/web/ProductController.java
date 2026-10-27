package edu.itc.se.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;
import edu.itc.se.shop.web.dto.CreateProduct;
import edu.itc.se.shop.web.dto.ProductDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    record StockUpdate(int delta) {}

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String q) {

        List<Product> products = (q == null || q.isBlank())
                ? repo.findAll()
                : repo.findByNameContaining(q);

        return products.stream()
                .map(ProductDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable long id) {

        return repo.findById(id)
                .map(ProductDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProduct request) {

        Product product = new Product(
            null,
            request.name(),
            request.price(),
            request.stock()
        );

        long id = repo.insert(product);

        Product created = new Product(
            id,
            request.name(),
            request.price(),
            request.stock()
        );

        return ResponseEntity
                .created(URI.create("/api/products/" + id))
                .body(ProductDto.from(created));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable long id,
            @RequestBody StockUpdate request) {

        int updated = repo.updateStock(id, request.delta());

        if (updated == 0) {
            return ResponseEntity.status(409).build();
        }

        return repo.findById(id)
                .map(ProductDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id) {

        return repo.delete(id) > 0
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}