package edu.itc.shop.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.*;

import edu.itc.shop.domain.Product;

@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductDto {
    @XmlAttribute public long id;

    @NotBlank @Size(max = 80)
    public String name;

    @NotNull @Pattern(regexp = "[A-Z]{2}-\\d{2}")
    public String sku;

    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
    public BigDecimal price;

    @PositiveOrZero
    public int stock;

    public ProductDto() { }

    public static ProductDto from(Product p) {
        var d = new ProductDto();
        d.id = p.id(); d.name = p.name(); d.sku = p.sku(); d.price = p.price(); d.stock = p.stock();
        return d;
    }

    public Product toProduct(long id, int version) {
        return new Product(id, name, sku, price, stock, version);
    }
}