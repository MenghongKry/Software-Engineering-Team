package edu.itc.se.shop.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties("shop")
public record ShopProperties(
        @NotBlank String currency,
        @NotNull @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal taxRate,
        @PositiveOrZero int lowStockThreshold) {}
