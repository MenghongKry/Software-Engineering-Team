package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

/** Challenge: one row of GET /api/reports/products (built with cb.construct). */
public record ProductReportRow(String sku, String name, BigDecimal amount, int stock, int tagCount) { }
