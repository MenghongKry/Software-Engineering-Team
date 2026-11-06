package edu.itc.se.shop_jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ProductMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void productMappingRoundTrip() {
        Product product = new Product(
            "SKU-001",
            "Keyboard",
            Money.eur("49.99"),
            Category.HARDWARE
        );

        product.setStock(10);

        entityManager.persist(product);
        entityManager.flush();

        Long id = product.getId();

        entityManager.clear();

        Product loaded = entityManager.find(Product.class, id);

        assertNotNull(loaded);
        assertEquals("SKU-001", loaded.getSku());
        assertEquals("Keyboard", loaded.getName());
        assertEquals(Money.eur("49.99"), loaded.getPrice());
        assertEquals(Category.HARDWARE, loaded.getCategory());
        assertEquals(10, loaded.getStock());

        Object category = entityManager
            .createNativeQuery(
                "SELECT category FROM product WHERE id = :id"
            )
            .setParameter("id", id)
            .getSingleResult();

        assertEquals("HARDWARE", category);
        assertTrue(category instanceof String);
    }
}