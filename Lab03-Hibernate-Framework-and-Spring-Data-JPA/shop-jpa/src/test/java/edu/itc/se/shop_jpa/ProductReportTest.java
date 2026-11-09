package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.repository.ProductReportFilter;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;
import jakarta.persistence.EntityManager;

/** Challenge: Criteria API report with optional filters, tag join, sort whitelist, projection. */
@SpringBootTest
@Transactional                       // everything is rolled back after each test
class ProductReportTest {

    @Autowired EntityManager em;
    @Autowired ProductRepository products;

    @BeforeEach
    void seed() {
        Tag sale = new Tag("rpt-sale");
        Tag fresh = new Tag("rpt-new");
        em.persist(sale);
        em.persist(fresh);
        product("RPT-KB1", "Rpt keyboard basic", "39.00", Category.HARDWARE, 10, sale);
        product("RPT-KB7", "Rpt mechanical keyboard", "129.00", Category.HARDWARE, 4, sale, fresh);
        product("RPT-MS2", "Rpt wireless mouse", "19.90", Category.HARDWARE, 0, sale);
        product("RPT-BK1", "Rpt clean code", "34.50", Category.BOOKS, 12);
        em.flush();
        em.clear();
    }

    @Test
    void allFiveFiltersAndSortTogether() {
        List<ProductReportRow> rows = report(new ProductReportFilter(
                "rpt", Category.HARDWARE, new BigDecimal("200"), 1, "rpt-sale", "amount,desc"));

        assertThat(rows).extracting(ProductReportRow::sku).containsExactly("RPT-KB7", "RPT-KB1");
        ProductReportRow kb7 = rows.getFirst();
        assertThat(kb7.name()).isEqualTo("Rpt mechanical keyboard");
        assertThat(kb7.amount()).isEqualByComparingTo("129.00");
        assertThat(kb7.stock()).isEqualTo(4);
        assertThat(kb7.tagCount()).isEqualTo(2);   // counts ALL its tags, not only "rpt-sale"
    }

    @Test
    void withoutTagFilterProductsWithoutTagsAreIncluded() {
        List<ProductReportRow> rows = report(new ProductReportFilter("rpt", null, null, null, null, "sku"));

        assertThat(rows).extracting(ProductReportRow::sku)
                .containsExactly("RPT-BK1", "RPT-KB1", "RPT-KB7", "RPT-MS2");
        assertThat(rows).filteredOn(r -> r.sku().equals("RPT-BK1"))
                .singleElement().extracting(ProductReportRow::tagCount).isEqualTo(0);
    }

    @Test
    void anySubsetOfFiltersWorks() {
        assertThat(report(new ProductReportFilter("rpt", Category.BOOKS, null, null, null, null)))
                .extracting(ProductReportRow::sku).containsExactly("RPT-BK1");
        assertThat(report(new ProductReportFilter("rpt", null, new BigDecimal("35"), null, null, null)))
                .extracting(ProductReportRow::sku).containsExactly("RPT-BK1", "RPT-MS2");
        assertThat(report(new ProductReportFilter("rpt", null, null, 5, null, "stock,desc")))
                .extracting(ProductReportRow::sku).containsExactly("RPT-BK1", "RPT-KB1");
        assertThat(report(new ProductReportFilter(null, null, null, null, "rpt-new", null)))
                .extracting(ProductReportRow::sku).containsExactly("RPT-KB7");
    }

    @Test
    void sortByTagCount() {
        assertThat(report(new ProductReportFilter("rpt", Category.HARDWARE, null, null, null, "tagCount,desc")))
                .first().extracting(ProductReportRow::sku).isEqualTo("RPT-KB7");
    }

    @Test
    void unknownSortColumnIsRejected() {
        assertThatThrownBy(() -> report(new ProductReportFilter(null, null, null, null, null, "version,desc")))
                // Spring's repository proxy translates exceptions into its DataAccessException family
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot sort by 'version'");
    }

    private List<ProductReportRow> report(ProductReportFilter filter) {
        return products.report(filter);
    }

    private void product(String sku, String name, String price, Category c, int stock, Tag... tags) {
        Product p = new Product(sku, name, Money.eur(price), c);
        p.setStock(stock);
        for (Tag t : tags) p.tag(t);
        em.persist(p);
    }
}
