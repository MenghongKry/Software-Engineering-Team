package edu.itc.se.shop_jpa.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import jakarta.persistence.EntityManager;

/**
 * Test helper for Task 5. All @SpringBootTest classes share ONE in-memory H2 database
 * (jdbc:h2:mem:shop;DB_CLOSE_DELAY=-1), so data committed by one test class is still there
 * for the next one. {@link #clear()} wipes every order so statement counts are exact.
 */
@Component
public class TestData {

    private final EntityManager em;
    private final TransactionTemplate tx;
    private final JdbcTemplate jdbc;

    public TestData(EntityManager em, TransactionTemplate tx, JdbcTemplate jdbc) {
        this.em = em;
        this.tx = tx;
        this.jdbc = jdbc;
    }

    /**
     * Creates {@code orders} PAID orders with {@code linesPerOrder} lines each.
     * Every line gets its own product, so loading product names lazily really costs one
     * SELECT per line. Idempotent: the previous data set is removed first.
     */
    public void paidOrders(int orders, int linesPerOrder) {
        clear();
        tx.executeWithoutResult(status -> {
            Address a = new Address("1 Test St", "Phnom Penh", "12000");
            Customer c = new Customer("Test Data", "testdata@shop.test", a, a);
            em.persist(c);
            for (int i = 0; i < orders; i++) {
                Order o = new Order(c);
                o.setStatus(OrderStatus.PAID);
                for (int j = 0; j < linesPerOrder; j++) {
                    Product p = new Product("TD-%d-%d".formatted(i, j), "Product %d-%d".formatted(i, j),
                            Money.eur("9.99"), Category.BOOKS);
                    p.setStock(10);
                    em.persist(p);
                    o.addLine(p, 1);
                }
                em.persist(o);          // cascades to the lines
            }
        });
    }

    /** Deletes all orders/lines, plus the customer and products created by this helper. */
    public void clear() {
        jdbc.update("delete from order_line");
        jdbc.update("delete from orders");
        jdbc.update("delete from product where sku like 'TD-%'");
        jdbc.update("delete from customer where email = 'testdata@shop.test'");
    }
}
