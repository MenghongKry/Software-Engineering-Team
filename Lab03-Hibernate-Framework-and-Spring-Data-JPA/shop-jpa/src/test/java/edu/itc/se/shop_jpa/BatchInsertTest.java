package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.support.QueryCounter;

/**
 * Task 5: with hibernate.jdbc.batch_size=50 (application.yml), 1 000 inserts are sent in
 * batches of 50, so Hibernate prepares far fewer than 1 000 statements.
 * The batch TRACE log shows "Executing JDBC batch"; per-row SQL/bind logging is switched off
 * here to keep the output readable.
 */
@SpringBootTest(properties = {
        "logging.level.org.hibernate.orm.jdbc.batch=TRACE",   // Hibernate 7 logger name
        "logging.level.org.hibernate.SQL=INFO",
        "logging.level.org.hibernate.orm.jdbc.bind=INFO"
})
class BatchInsertTest {

    static final int COUNT = 1_000;

    @Autowired ProductRepository products;
    @Autowired TransactionTemplate tx;
    @Autowired QueryCounter counter;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() { jdbc.update("delete from product where sku like 'BULK-%'"); }

    @Test
    void saveAllUsesJdbcBatches() {
        List<Product> list = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            Product p = new Product("BULK-" + i, "Bulk product " + i, Money.eur("1.00"), Category.FOOD);
            p.setStock(1);
            list.add(p);
        }

        counter.reset();
        tx.executeWithoutResult(s -> products.saveAll(list));

        long statements = counter.count();
        System.out.printf("BatchInsertTest: %d inserts, %d prepared statements%n", counter.inserts(), statements);

        assertThat(counter.inserts()).isEqualTo(COUNT);
        // 20 "next value for product_seq" (allocationSize 50) + 20 insert batches (batch_size 50)
        assertThat(statements).isLessThan(100);
    }
}
