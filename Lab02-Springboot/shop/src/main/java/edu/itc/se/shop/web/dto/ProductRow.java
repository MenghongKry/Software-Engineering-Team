package edu.itc.se.shop.web.dto;

import java.math.BigDecimal;

import edu.itc.se.shop.domain.Product;

public class ProductRow {
    private final Long id;
    private final String name;
    private final BigDecimal price;
    private final BigDecimal gross;
    private final int stock;

    public ProductRow(Product product, BigDecimal gross) {
        this.id = product.id();
        this.name = product.name();
        this.price = product.price();
        this.gross = gross;
        this.stock = product.stock();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getGross() { return gross; }
    public int getStock() { return stock; }
}
