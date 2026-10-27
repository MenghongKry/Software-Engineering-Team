package edu.itc.se.shop.web.dto;

import java.math.BigDecimal;
import edu.itc.se.shop.domain.Product;

public record ProductDto(long id, String name, BigDecimal price, int stock) {

  public static ProductDto from(Product product) {
    return new ProductDto(
      product.id(),
      product.name(),
      product.price(),
      product.stock()
    ); 
  }
}