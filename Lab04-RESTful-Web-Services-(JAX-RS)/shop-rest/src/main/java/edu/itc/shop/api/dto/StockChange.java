package edu.itc.shop.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StockChange(@Min(-1000) @Max(1000) int delta) { }