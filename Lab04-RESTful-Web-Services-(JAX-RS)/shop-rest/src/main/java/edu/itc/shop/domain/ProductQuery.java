package edu.itc.shop.domain;

public record ProductQuery(String text,
                           Money minPrice,
                           Money maxPrice,
                           boolean inStockOnly,
                           String sortField,
                           boolean descending) {
}