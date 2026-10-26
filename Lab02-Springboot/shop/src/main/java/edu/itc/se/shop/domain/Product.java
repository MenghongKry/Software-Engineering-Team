package edu.itc.se.shop.domain;

import java.math.BigDecimal;


public record Product(Long id, String name, BigDecimal price, int stock) {}
