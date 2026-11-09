package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Task 5: V2__indexes.sql is applied after V1, and H2 uses the new indexes.
 * The EXPLAIN output is printed so it can be copied into README-Task5.md.
 */
@SpringBootTest
class MigrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayHistoryHasBothVersions() {
        List<String> rows = jdbc.queryForList(
                "select \"version\" || ' - ' || \"description\" from \"flyway_schema_history\""
                        + " where \"version\" is not null and \"success\" order by \"installed_rank\"",
                String.class);

        assertThat(rows).containsExactly("1 - init", "2 - indexes");
    }

    @Test
    void productSearchUsesTheNewCategoryAmountIndex() {
        String plan = explain("select * from product where category = 'HARDWARE' and amount <= 60");

        assertThat(plan).containsIgnoringCase("IDX_PRODUCT_CATEGORY_AMOUNT");
    }

    @Test
    void printPlansForReadme() {
        System.out.println("EXPLAIN /api/orders:\n" + explain("""
                select o.id, o.status, o.placed_at, c.name from orders o
                  join customer c on c.id = o.customer_id
                 where o.status = 'PAID' order by o.placed_at desc
                 offset 0 rows fetch first 5 rows only"""));
        System.out.println("EXPLAIN orders of a customer:\n" + explain("""
                select o.id, o.status, o.placed_at from orders o
                 where o.customer_id = 1 order by o.placed_at desc"""));
        System.out.println("EXPLAIN lines of an order:\n"
                + explain("select * from order_line where order_id = 1"));
        System.out.println("EXPLAIN products by category:\n"
                + explain("select * from product where category = 'HARDWARE' and amount <= 60"));
    }

    private String explain(String sql) {
        return jdbc.queryForObject("EXPLAIN " + sql, String.class);
    }
}
