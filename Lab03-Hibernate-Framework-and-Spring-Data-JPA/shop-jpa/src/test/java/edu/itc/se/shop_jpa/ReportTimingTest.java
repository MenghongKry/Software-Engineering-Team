package edu.itc.se.shop_jpa;

import static edu.itc.se.shop_jpa.NPlusOneTest.LINES;
import static edu.itc.se.shop_jpa.NPlusOneTest.ORDERS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.se.shop_jpa.service.ReportService;
import edu.itc.se.shop_jpa.support.QueryCounter;
import edu.itc.se.shop_jpa.support.TestData;

/**
 * Task 5: measures every variant 20 times after a warm-up and prints a Markdown table
 * (variant | statements | median ms) that is copied into README-Task5.md.
 * Tagged "demo": the numbers depend on the machine, so nothing about time is asserted.
 */
@SpringBootTest
@Tag("demo")
class ReportTimingTest {

    static final int WARM_UP = 5;
    static final int RUNS = 20;

    @Autowired ReportService report;
    @Autowired QueryCounter counter;
    @Autowired TestData data;

    @BeforeEach
    void seed() { data.paidOrders(ORDERS, LINES); }

    @AfterEach
    void cleanUp() { data.clear(); }

    @Test
    void printTimingTable() {
        Map<String, Runnable> variants = new LinkedHashMap<>();
        variants.put("naive (lazy loop)", report::linesPerPaidOrder);
        variants.put("join fetch", report::linesPerPaidOrderJoinFetch);
        variants.put("@EntityGraph", report::linesPerPaidOrderEntityGraph);
        variants.put("batch size 16", report::linesPerPaidOrderBatched);
        variants.put("naive + product name", report::productNamesPerPaidOrder);
        variants.put("join fetch + product name", report::productNamesPerPaidOrderJoinFetch);
        variants.put("@EntityGraph + product name", report::productNamesPerPaidOrderEntityGraph);
        variants.put("batch size 16 + product name", report::productNamesPerPaidOrderBatched);

        StringBuilder table = new StringBuilder("""

                | variant | statements | median ms |
                |---|---:|---:|
                """);
        variants.forEach((name, run) -> {
            for (int i = 0; i < WARM_UP; i++) run.run();

            counter.reset();
            run.run();
            long statements = counter.count();

            double[] ms = new double[RUNS];
            for (int i = 0; i < RUNS; i++) {
                long t0 = System.nanoTime();
                run.run();
                ms[i] = (System.nanoTime() - t0) / 1_000_000.0;
            }
            table.append("| %s | %d | %.1f |%n".formatted(name, statements, median(ms)));
            assertThat(statements).isPositive();
        });
        System.out.println(table);
    }

    private static double median(double[] values) {
        double[] v = values.clone();
        Arrays.sort(v);
        return (v[v.length / 2 - 1] + v[v.length / 2]) / 2;
    }
}
