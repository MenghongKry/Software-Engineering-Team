package edu.itc.se.shop_jpa.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.repository.ProductReportFilter;
import edu.itc.se.shop_jpa.service.ProductReportService;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

/** Challenge: GET /api/reports/products?q=&category=&maxPrice=&minStock=&tag=&sort=amount,desc */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ProductReportService reports;

    public ReportController(ProductReportService reports) { this.reports = reports; }

    @GetMapping("/products")
    public List<ProductReportRow> products(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Category category,
                                          @RequestParam(required = false) BigDecimal maxPrice,
                                          @RequestParam(required = false) Integer minStock,
                                          @RequestParam(required = false) String tag,
                                          @RequestParam(required = false) String sort) {
        return reports.report(new ProductReportFilter(q, category, maxPrice, minStock, tag, sort));
    }
}
