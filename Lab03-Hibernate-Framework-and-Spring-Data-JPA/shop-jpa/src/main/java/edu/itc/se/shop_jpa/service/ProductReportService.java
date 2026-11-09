package edu.itc.se.shop_jpa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.repository.ProductReportFilter;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

@Service
public class ProductReportService {

    private final ProductRepository products;

    public ProductReportService(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public List<ProductReportRow> report(ProductReportFilter filter) {
        return products.report(filter);
    }
}
