package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.itc.se.shop_jpa.service.BulkImportService;
import edu.itc.se.shop_jpa.support.QueryCounter;

/**
 * Challenge: import 50 000 products from a CSV three ways and compare wall time and heap.
 * SQL logging is switched off here: logging 50 000 INSERTs would take longer than the import.
 */
@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=INFO",
        "logging.level.org.hibernate.orm.jdbc.bind=INFO"
})
class BulkImportTest {

    static final Logger log = LoggerFactory.getLogger(BulkImportTest.class);
    static final int ROWS = 50_000;

    @TempDir static Path dir;
    static Path csv;

    @Autowired BulkImportService importer;
    @Autowired JdbcTemplate jdbc;
    @Autowired QueryCounter counter;

    @BeforeAll
    static void writeCsv() throws IOException {
        csv = dir.resolve("catalogue.csv");
        String[] categories = {"BOOKS", "HARDWARE", "TOYS", "FOOD"};
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("sku;name;amount;currency;category;stock\n");
            for (int i = 0; i < ROWS; i++) {
                w.write("IMP-%06d;Imported product %d;%d.99;EUR;%s;%d%n"
                        .formatted(i, i, 1 + i % 200, categories[i % 4], i % 50));
            }
        }
    }

    @AfterEach
    void cleanUp() { jdbc.update("delete from product where sku like 'IMP-%'"); }

    @Test
    void statelessSessionInsertMultiple() throws Exception {
        Measured m = measure(() -> importer.importCsv(csv));
        log.info("stateless insertMultiple(500): {} rows in {} s, {} statements, heap +{} MB",
                m.rows, m.seconds(), m.statements, m.heapMb());
        assertImported(m);
        assertThat(m.millis).isLessThan(10_000);                     // the challenge target
    }

    @Test
    void saveAllOneTransactionNoClear() throws Exception {
        Measured m = measure(() -> importer.importCsvWithSaveAll(csv, false));
        log.info("saveAll, one tx, no clear   : {} rows in {} s, heap +{} MB", m.rows, m.seconds(), m.heapMb());
        assertImported(m);
    }

    @Test
    void saveAllWithFlushAndClearEvery500() throws Exception {
        Measured m = measure(() -> importer.importCsvWithSaveAll(csv, true));
        log.info("saveAll + flush/clear(500)  : {} rows in {} s, heap +{} MB", m.rows, m.seconds(), m.heapMb());
        assertImported(m);
    }

    private void assertImported(Measured m) {
        assertThat(m.rows).isEqualTo(ROWS);
        assertThat(jdbc.queryForObject("select count(*) from product where sku like 'IMP-%'", Long.class))
                .isEqualTo(ROWS);
    }

    // ---------- measuring ----------

    record Measured(int rows, long millis, long peakHeapBytes, long statements) {
        String seconds() { return "%.1f".formatted(millis / 1000.0); }
        long heapMb() { return peakHeapBytes / (1024 * 1024); }
    }

    /** Runs the import while a background thread samples the used heap every 5 ms. */
    private Measured measure(Callable<Integer> run) throws Exception {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        System.gc();
        long before = memory.getHeapMemoryUsage().getUsed();
        AtomicLong peak = new AtomicLong(before);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread sampler = Thread.ofPlatform().daemon().start(() -> {
            while (running.get()) {
                peak.accumulateAndGet(memory.getHeapMemoryUsage().getUsed(), Math::max);
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
            }
        });
        counter.reset();
        long t0 = System.nanoTime();
        int rows;
        try {
            rows = run.call();
        } finally {
            running.set(false);
            sampler.join();
        }
        long millis = (System.nanoTime() - t0) / 1_000_000;
        return new Measured(rows, millis, Math.max(0, peak.get() - before), counter.count());
    }
}
