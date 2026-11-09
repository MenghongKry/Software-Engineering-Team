package edu.itc.se.shop_jpa.repository;

import java.math.BigDecimal;

import edu.itc.se.shop_jpa.domain.Category;

public record ProductReportFilter(String q, Category category, BigDecimal maxPrice,
                                  Integer minStock, String tag, String sort) { }
