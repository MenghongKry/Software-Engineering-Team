package edu.itc.se.shop.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ProductForm {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
