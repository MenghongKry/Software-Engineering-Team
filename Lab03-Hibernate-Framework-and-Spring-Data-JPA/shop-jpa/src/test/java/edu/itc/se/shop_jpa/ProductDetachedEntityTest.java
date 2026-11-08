package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

@SpringBootTest
class ProductDetachedEntityTest {

    @Autowired ProductRepository products;

    @Test
    void detachedChangesAreNotPersistedUntilSaved() {
        Product original = products.saveAndFlush(new Product(
                "DET-" + UUID.randomUUID().toString().substring(0, 12),
                "Detached product", Money.eur("10.00"), Category.BOOKS));
        Long id = original.getId();

        Product detached = products.findById(id).orElseThrow();
        detached.setStock(99);

        assertThat(products.findById(id).orElseThrow().getStock()).isZero();

        products.save(detached);

        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(99);
    }
}
