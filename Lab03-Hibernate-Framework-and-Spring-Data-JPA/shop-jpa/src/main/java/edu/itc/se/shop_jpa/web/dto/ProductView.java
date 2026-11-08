package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

public record ProductView(Long id, String sku, String name, BigDecimal amount, int stock) { }