package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.se.shop_jpa.service.ReportService;
import edu.itc.se.shop_jpa.support.QueryCounter;
import edu.itc.se.shop_jpa.support.TestData;

/**
 * Task 5: the N+1 problem, proven by counting JDBC statements.
 * 50 PAID orders x 3 lines, every line with its own product.
 */
@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class NPlusOneTest {

    static final int ORDERS = 50;
    static final int LINES = 3;

    @Autowired ReportService report;
    @Autowired QueryCounter counter;
    @Autowired TestData data;

    @BeforeEach
    void seed() { data.paidOrders(ORDERS, LINES); }

    @AfterEach
    void cleanUp() { data.clear(); }

    @Test @Tag("demo")
    void naiveImplementationRunsNPlusOneQueries() {
        counter.reset();
        Map<Long, Integer> result = report.linesPerPaidOrder();

        assertThat(result).hasSize(ORDERS).allSatisfy((id, n) -> assertThat(n).isEqualTo(LINES));
        assertThat(counter.count()).isEqualTo(1 + ORDERS);                     // 51
    }

    @Test
    void joinFetchRunsOneQuery() {
        counter.reset();
        Map<Long, Integer> result = report.linesPerPaidOrderJoinFetch();

        assertThat(result).hasSize(ORDERS).allSatisfy((id, n) -> assertThat(n).isEqualTo(LINES));
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void entityGraphRunsOneQuery() {
        counter.reset();
        Map<Long, Integer> result = report.linesPerPaidOrderEntityGraph();

        assertThat(result).hasSize(ORDERS).allSatisfy((id, n) -> assertThat(n).isEqualTo(LINES));
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void batchSizeRunsOnePlusCeilNOverBatch() {
        counter.reset();
        Map<Long, Integer> result = report.linesPerPaidOrderBatched();

        assertThat(result).hasSize(ORDERS).allSatisfy((id, n) -> assertThat(n).isEqualTo(LINES));
        assertThat(counter.count())
                .isEqualTo(1 + (int) Math.ceil(ORDERS / (double) ReportService.BATCH_SIZE));   // 5
    }

    // ---- adding line.getProduct().getName() to the report ----

    @Test @Tag("demo")
    void productNameMakesNaiveEvenWorse() {
        counter.reset();
        report.productNamesPerPaidOrder();
        // 1 (orders) + 50 (lines) + 150 (one product per line)
        assertThat(counter.count()).isEqualTo(1 + ORDERS + ORDERS * LINES);
    }

    @Test @Tag("demo")
    void productNameBringsNPlusOneBackToJoinFetch() {
        counter.reset();
        report.productNamesPerPaidOrderJoinFetch();
        // join fetch only covers o.lines, so each product is still loaded lazily: 1 + 150
        assertThat(counter.count()).isEqualTo(1 + ORDERS * LINES);
    }

    @Test
    void entityGraphWithLinesProductStaysAtOneQuery() {
        counter.reset();
        report.productNamesPerPaidOrderEntityGraph();
        assertThat(counter.count()).isEqualTo(1);       // "lines.product" is in the graph
    }

    @Test
    void batchingAlsoBatchesTheProducts() {
        counter.reset();
        report.productNamesPerPaidOrderBatched();
        int b = ReportService.BATCH_SIZE;
        // 1 + ceil(50/16) line batches + ceil(150/16) product batches = 1 + 4 + 10
        assertThat(counter.count()).isEqualTo(1 + ceil(ORDERS, b) + ceil(ORDERS * LINES, b));
    }

    static int ceil(int n, int batch) { return (n + batch - 1) / batch; }
}
