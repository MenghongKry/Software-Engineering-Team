package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.service.OrderFeedService;
import edu.itc.se.shop_jpa.support.QueryCounter;
import edu.itc.se.shop_jpa.support.TestData;
import edu.itc.se.shop_jpa.web.dto.OrderFeed;
import edu.itc.se.shop_jpa.web.dto.OrderFeedItem;
import jakarta.persistence.EntityManager;

/** Challenge: infinite-scroll feed, keyset and Slice versions must return the same orders. */
@SpringBootTest
class OrderFeedTest {

    static final int ORDERS = 45;
    static final Instant T0 = Instant.parse("2026-09-20T08:00:00Z");

    @Autowired OrderFeedService feed;
    @Autowired TestData data;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired QueryCounter counter;

    List<Long> newestFirst;

    @BeforeEach
    void seed() {
        data.clear();
        newestFirst = tx.execute(s -> {
            Address a = new Address("1 Feed St", "Phnom Penh", "12000");
            Customer c = new Customer("Feed Reader", "testdata@shop.test", a, a);
            em.persist(c);
            List<Order> created = new ArrayList<>();
            for (int i = 0; i < ORDERS; i++) {
                // pairs of orders share the same placedAt → the id must break the tie
                Order o = new Order(c, T0.plus(i / 2, ChronoUnit.MINUTES));
                em.persist(o);
                created.add(o);
            }
            em.flush();
            return created.reversed().stream().map(Order::getId).toList();
        });
    }

    @AfterEach
    void cleanUp() { data.clear(); }

    @Test
    void keysetFeedWalksThroughAllOrdersNewestFirst() {
        List<Long> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        OrderFeed page;
        do {
            page = feed.keyset(cursor, 20);
            page.items().forEach(i -> seen.add(i.id()));
            cursor = page.nextCursor();
            pages++;
        } while (page.hasNext());

        assertThat(pages).isEqualTo(3);                       // 20 + 20 + 5
        assertThat(seen).containsExactlyElementsOf(newestFirst);
    }

    @Test
    void cursorLooksLikePlacedAtCommaId() {
        OrderFeed first = feed.keyset(null, 20);
        OrderFeedItem last = first.items().getLast();

        assertThat(first.items()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        assertThat(first.nextCursor()).isEqualTo(last.placedAt() + "," + last.id());
        assertThat(last.customerName()).isEqualTo("Feed Reader");
    }

    @Test
    void keysetPageIsOneStatementEvenWithCustomerNames() {
        counter.reset();
        feed.keyset(null, 20);
        assertThat(counter.count()).isEqualTo(1);             // customer comes from the entity graph
    }

    @Test
    void sliceFeedReturnsTheSameOrder() {
        List<Long> seen = new ArrayList<>();
        int pageNo = 0;
        OrderFeed page;
        do {
            page = feed.slice(pageNo++, 20);
            page.items().forEach(i -> seen.add(i.id()));
        } while (page.hasNext());

        assertThat(seen).containsExactlyElementsOf(newestFirst);
    }
}
