package edu.itc.se.shop_jpa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import edu.itc.se.shop_jpa.web.dto.ProductView;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    // PriceAmount = price.amount (path into the @Embedded Money)
    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    // Tags = the @ManyToMany set → Spring adds a join to product_tag and tag
    List<Product> findByTagsName(String tagName);

    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock",
            nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);

    @Query(value = """
            select new edu.itc.se.shop_jpa.web.dto.ProductView(
                   p.id, p.sku, p.name, p.price.amount, p.stock)
              from Product p
             where p.category = :category
            """,
            countQuery = "select count(p) from Product p where p.category = :category")
    Page<ProductView> findByCategory(@Param("category") Category category, Pageable pageable);
}