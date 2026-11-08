package edu.itc.se.shop_jpa.web;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.service.ProductService;
import edu.itc.se.shop_jpa.web.dto.ProductView;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping
    public Page<ProductView> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Category category,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) Boolean inStock,
                                    @PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        return productService.search(q, category, maxPrice, inStock, pageable);
    }
}