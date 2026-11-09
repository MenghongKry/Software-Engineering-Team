package edu.itc.se.shop_jpa;

import static edu.itc.se.shop_jpa.NPlusOneTest.LINES;
import static edu.itc.se.shop_jpa.NPlusOneTest.ORDERS;
import static edu.itc.se.shop_jpa.NPlusOneTest.ceil;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.se.shop_jpa.service.ReportService;
import edu.itc.se.shop_jpa.support.QueryCounter;
import edu.itc.se.shop_jpa.support.TestData;

/**
 * Task 5: the global safety net. With hibernate.default_batch_fetch_size=32 every lazy
 * collection and every lazy proxy is loaded up to 32 at a time, so a forgotten fetch can no
 * longer explode into hundreds of statements. It runs in its own Spring context so that the
 * setting does not change the counts proven in NPlusOneTest.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=32")
class DefaultBatchFetchSizeTest {

    @Autowired ReportService report;
    @Autowired QueryCounter counter;
    @Autowired TestData data;

    @BeforeEach
    void seed() { data.paidOrders(ORDERS, LINES); }

    @AfterEach
    void cleanUp() { data.clear(); }

    @Test
    void naiveWithProductNamesIsCapped() {
        counter.reset();
        report.productNamesPerPaidOrder();
        // 1 + ceil(50/32) + ceil(150/32) = 1 + 2 + 5 = 8   (was 201)
        assertThat(counter.count()).isEqualTo(1 + ceil(ORDERS, 32) + ceil(ORDERS * LINES, 32));
    }

    @Test
    void joinFetchWithProductNamesIsCapped() {
        counter.reset();
        report.productNamesPerPaidOrderJoinFetch();
        // 1 + ceil(150/32) = 1 + 5 = 6   (was 151)
        assertThat(counter.count()).isEqualTo(1 + ceil(ORDERS * LINES, 32));
    }
}
