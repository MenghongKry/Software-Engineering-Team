package edu.itc.se.shop.service;

import java.util.List;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import edu.itc.se.shop.config.ShopProperties;
import edu.itc.se.shop.domain.Product;

@Component
public class LowStockHealthIndicator implements HealthIndicator {
    private final ProductService products;
    private final ShopProperties props;

    public LowStockHealthIndicator(ProductService products, ShopProperties props) {
        this.products = products;
        this.props = props;
    }

    @Override
    public Health health() {
        List<String> low = products.findAll(null).stream()
                .filter(p -> p.stock() < props.lowStockThreshold())
                .map(Product::name)
                .toList();
        if (low.isEmpty()) {
            return Health.up().build();
        }
        return Health.down().withDetail("lowStockProducts", low).build();
    }
}
