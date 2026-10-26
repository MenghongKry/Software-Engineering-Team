package edu.itc.se.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import edu.itc.se.shop.repository.ProductRepository;

@SpringBootTest
class ProductServiceTest {
    @Autowired ProductService service;
    @Autowired ProductRepository repo;

    private int stockOf(long id) {
        return repo.findById(id).orElseThrow().stock();
    }

    @Test
    void restockRollsBackWhenSourceHasTooLittleStock() {
        int before1 = stockOf(1);
        int before2 = stockOf(2);

        assertThrows(IllegalStateException.class, () -> service.restock(1, 2, 1_000));

        assertEquals(before1, stockOf(1));
        assertEquals(before2, stockOf(2));
    }

    @Test
    void firstUpdateIsRolledBackWhenTargetDoesNotExist() {
        int before1 = stockOf(1);

        // the first UPDATE succeeds, the second finds no row 999 and throws
        assertThrows(IllegalStateException.class, () -> service.restock(1, 999, 3));

        assertEquals(before1, stockOf(1));
    }

    @Test
    void restockMovesStockWhenEverythingIsFine() {
        int before3 = stockOf(3);
        int before4 = stockOf(4);

        service.restock(4, 3, 2);

        assertEquals(before3 + 2, stockOf(3));
        assertEquals(before4 - 2, stockOf(4));
    }
}
