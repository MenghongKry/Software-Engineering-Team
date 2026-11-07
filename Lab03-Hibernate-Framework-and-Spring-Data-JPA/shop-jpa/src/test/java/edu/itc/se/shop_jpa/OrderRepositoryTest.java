package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import edu.itc.se.shop_jpa.domain.*;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;
import edu.itc.se.shop_jpa.web.dto.ProductView;
import jakarta.persistence.PersistenceUnitUtil;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    private static final String ADA = "rt-ada@test.com";
    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired TestEntityManager tem;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;

    PersistenceUnitUtil util;
    Long adaId, oldPaidId, newPaidId, adaNewId;
    long paidBefore, paidLinesBefore;

    @BeforeEach
    void seed() {
        util = tem.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        paidBefore = orders.countByStatus(OrderStatus.PAID);          // shared DB → relative counts
        paidLinesBefore = orders.countLinesByStatus(OrderStatus.PAID);

        Address a = new Address("1 Main St", "London", "10000");
        Customer ada = tem.persist(new Customer("Ada", ADA, a, a));
        Customer bob = tem.persist(new Customer("Bob", "rt-bob@test.com", a, a));

        Product keyboard = product("RT-KB", "Keyboard", "50.00", Category.HARDWARE, 2);
        Product mouse    = product("RT-MS", "Mouse",    "20.00", Category.HARDWARE, 30);
        Product book     = product("RT-BK", "Book",     "35.00", Category.BOOKS,    0);

        Order oldPaid = order(ada, T0, OrderStatus.PAID);
        oldPaid.addLine(keyboard, 1);
        oldPaid.addLine(mouse, 2);
        Order newPaid = order(ada, T0.plus(1, ChronoUnit.DAYS), OrderStatus.PAID);
        newPaid.addLine(book, 1);
        Order adaNew = order(ada, T0.plus(2, ChronoUnit.DAYS), OrderStatus.NEW);
        adaNew.addLine(mouse, 1);
        Order bobPaid = order(bob, T0.plus(3, ChronoUnit.DAYS), OrderStatus.PAID);
        bobPaid.addLine(keyboard, 1);

        List.of(oldPaid, newPaid, adaNew, bobPaid).forEach(tem::persist);
        tem.flush();
        adaId = ada.getId();
        oldPaidId = oldPaid.getId();
        newPaidId = newPaid.getId();
        adaNewId = adaNew.getId();
        tem.clear();                         // every test starts with an empty persistence context
    }

    @Test
    void derivedQueries() {
        assertThat(orders.findByCustomerEmailAndStatusOrderByPlacedAtDesc(ADA, OrderStatus.PAID))
                .extracting(Order::getId)
                .containsExactly(newPaidId, oldPaidId);                    // newest first

        assertThat(orders.countByStatus(OrderStatus.PAID)).isEqualTo(paidBefore + 3);
        assertThat(orders.countLinesByStatus(OrderStatus.PAID)).isEqualTo(paidLinesBefore + 4);

        assertThat(orders.findFirstByCustomerIdOrderByPlacedAtDesc(adaId))
                .get().extracting(Order::getId).isEqualTo(adaNewId);
    }

    @Test
    void fetchJoinInitialisesLinesAndProducts() {
        List<Order> result = orders.findWithLinesByCustomerEmail(ADA);

        assertThat(result).extracting(Order::getId).containsExactly(adaNewId, newPaidId, oldPaidId);
        assertThat(result).allSatisfy(o -> {
            assertThat(util.isLoaded(o, "lines")).isTrue();
            assertThat(o.getLines()).allSatisfy(l ->
                    assertThat(Hibernate.isInitialized(l.getProduct())).isTrue());
        });
    }

    @Test
    void plainFindByIdLeavesLinesLazy() {
        Order o = orders.findById(oldPaidId).orElseThrow();   // context was cleared in seed()

        assertThat(util.isLoaded(o, "lines")).isFalse();      // nothing loaded yet
        assertThat(o.getLines()).hasSize(2);                   // touching it triggers the 2nd SELECT
        assertThat(util.isLoaded(o, "lines")).isTrue();
    }

    @Test
    void nativeLowStockQuery() {
        List<Product> low = products.findLowStock(5);

        assertThat(low).extracting(Product::getSku)
                .contains("RT-BK", "RT-KB")
                .doesNotContain("RT-MS");
        assertThat(low).isSortedAccordingTo(Comparator.comparingInt(Product::getStock));
    }

    @Test
    void interfaceAndRecordProjections() {
        Page<OrderSummary> page = orders.findByStatus(OrderStatus.PAID,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "placedAt")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(paidBefore + 3);
        assertThat(page.getContent()).allSatisfy(s -> {
            assertThat(s.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(s.getCustomer().name()).isNotBlank();
        });

        Page<ProductView> books = products.findByCategory(Category.BOOKS, PageRequest.of(0, 100));
        assertThat(books.getContent())
                .filteredOn(v -> v.sku().equals("RT-BK"))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.amount()).isEqualByComparingTo("35.00");
                    assertThat(v.stock()).isZero();
                });
    }

    // --- helpers ---
    private Product product(String sku, String name, String price, Category c, int stock) {
        Product p = new Product(sku, name, Money.eur(price), c);
        p.setStock(stock);
        return tem.persist(p);
    }

    private static Order order(Customer c, Instant at, OrderStatus status) {
        Order o = new Order(c, at);
        o.setStatus(status);
        return o;
    }
}