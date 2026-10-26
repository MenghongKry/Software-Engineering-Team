package edu.itc.se.shop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import edu.itc.se.shop.domain.Product;

/** Starts only the database part of Spring; every test is rolled back at the end. */
@JdbcTest
@Import({ProductRepository.class, RawJdbcProductRepository.class})
class ProductRepositoryTest {
    @Autowired ProductRepository repo;
    @Autowired RawJdbcProductRepository rawRepo;

    @Test
    void findAllReturnsTheFiveSeedProducts() {
        assertEquals(5, repo.findAll().size());
        assertEquals(repo.findAll(), rawRepo.findAll());
    }

    @Test
    void findByIdAndFindByNameContaining() {
        assertEquals("Keyboard", repo.findById(1).orElseThrow().name());
        assertTrue(repo.findById(999).isEmpty());
        assertEquals(1, repo.findByNameContaining("eyb").size());
    }

    @Test
    void insertReturnsGeneratedIdAndDeleteRemovesTheRow() {
        long id = repo.insert(new Product(null, "Webcam", new BigDecimal("39.90"), 7));

        assertEquals("Webcam", repo.findById(id).orElseThrow().name());
        assertEquals(1, repo.delete(id));
        assertTrue(repo.findById(id).isEmpty());
    }

    @Test
    void updateStockRefusesToGoBelowZero() {
        assertEquals(1, repo.updateStock(1, -3));
        assertEquals(7, repo.findById(1).orElseThrow().stock());
        assertEquals(0, repo.updateStock(1, -100));
        assertEquals(0, repo.updateStock(999, 1));
    }

    @Test
    void duplicateNameIsTranslatedToDuplicateKeyException() {
        var duplicate = new Product(null, "Keyboard", new BigDecimal("1.00"), 1);

        DuplicateKeyException e = assertThrows(DuplicateKeyException.class, () -> repo.insert(duplicate));
        System.out.println("DUPLICATE -> " + e.getClass().getName());
        System.out.println("  cause   -> " + e.getCause().getClass().getName());
    }
}
