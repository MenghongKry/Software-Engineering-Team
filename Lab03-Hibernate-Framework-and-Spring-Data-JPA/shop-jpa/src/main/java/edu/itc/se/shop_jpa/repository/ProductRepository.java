package edu.itc.se.shop_jpa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    // PriceAmount = price.amount (path into the @Embedded Money)
    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    // Tags = the @ManyToMany set → Spring adds a join to product_tag and tag
    List<Product> findByTagsName(String tagName);

    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock",
            nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);
}