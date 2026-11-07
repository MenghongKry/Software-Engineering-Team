package edu.itc.se.shop_jpa.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.repository.ProductSpecs;
import edu.itc.se.shop_jpa.web.dto.ProductView;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public Page<ProductView> search(String q, Category category, BigDecimal maxPrice,
                                    Boolean inStock, Pageable pageable) {
        return products.findAll(ProductSpecs.filter(q, category, maxPrice, inStock), pageable)
                .map(ProductService::toView);          // entity → record, inside the tx
    }

    private static ProductView toView(Product p) {
        return new ProductView(p.getId(), p.getSku(), p.getName(),
                p.getPrice().amount(), p.getStock());
    }
}