package edu.itc.se.shop_jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.Product;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class OrderCascadeTest {

    @Autowired
    private EntityManager em;

    private Customer customer;
    private Product p1;
    private Product p2;

    @BeforeEach
    void setUp() {
        Address addr = new Address("Street 1", "Phnom Penh", "12000");
        customer = new Customer("Sok", "sok@itc.edu.kh", addr, addr);
        p1 = new Product("SKU-A", "Book", Money.eur("10.00"), Category.BOOKS);
        p2 = new Product("SKU-B", "Toy", Money.eur("5.50"), Category.TOYS);
        em.persist(customer);
        em.persist(p1);
        em.persist(p2);
        em.flush();
    }

    private long countLines() {
        return em.createQuery("select count(l) from OrderLine l", Long.class)
                .getSingleResult();
    }

    private Long saveOrderWithTwoLines() {
        Order order = new Order(customer);
        order.addLine(p1, 2);
        order.addLine(p2, 1);
        em.persist(order);
        em.flush();            // log: 1 insert orders + 2 insert order_line, no update
        Long id = order.getId();
        em.clear();
        return id;
    }

    @Test
    void persistOrderCascadesToLines() {
        Long id = saveOrderWithTwoLines();

        Order loaded = em.find(Order.class, id);
        assertEquals(2, loaded.getLines().size());
        assertEquals(0, loaded.total().amount().compareTo(new BigDecimal("25.50")));
    }

    @Test
    void removeLineDeletesOrphan() {
        Long id = saveOrderWithTwoLines();

        Order loaded = em.find(Order.class, id);
        long before = countLines();
        loaded.removeLine(loaded.getLines().get(0));
        em.flush();            // log: delete from order_line where id=?

        assertEquals(before - 1, countLines());
    }

    @Test
    void removeOrderCascadesToLines() {
        Long id = saveOrderWithTwoLines();

        Order loaded = em.find(Order.class, id);
        em.remove(loaded);
        em.flush();

        assertEquals(0, countLines());
    }
}