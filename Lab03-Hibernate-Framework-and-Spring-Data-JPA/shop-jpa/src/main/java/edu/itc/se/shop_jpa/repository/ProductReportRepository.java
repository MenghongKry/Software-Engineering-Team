package edu.itc.se.shop_jpa.repository;

import java.util.List;

import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

public interface ProductReportRepository {
    List<ProductReportRow> report(ProductReportFilter filter);
}
