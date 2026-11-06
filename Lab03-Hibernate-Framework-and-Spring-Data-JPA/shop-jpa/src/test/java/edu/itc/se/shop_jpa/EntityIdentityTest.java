package edu.itc.se.shop_jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

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
class EntityIdentityTest {

    @Autowired
    private EntityManager em;

    @Test
    void hashSetSurvivesPersist() {
        Product p = new Product("SKU-HS", "Pen", Money.eur("1.20"), Category.BOOKS);
        Set<Product> set = new HashSet<>();
        set.add(p);                      // id is still null here

        em.persist(p);
        em.flush();                      // id is assigned now

        assertTrue(set.contains(p));     // works because hashCode is constant
    }

    @Test
    void equalsByIdButNotSameReference() {
        Product p = new Product("SKU-EQ", "Ruler", Money.eur("2.00"), Category.BOOKS);
        em.persist(p);
        em.flush();
        Long id = p.getId();
        em.clear();

        Product reloaded = em.find(Product.class, id);
        assertEquals(p, reloaded);
        assertNotSame(p, reloaded);
    }
}