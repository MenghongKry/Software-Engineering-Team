package edu.itc.se.shop_jpa.domain;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Money(
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount,

    @Column(name = "currency", nullable = false, length = 3)
    String currency
) {
    public static Money eur(String value) {
        return new Money(new BigDecimal(value), "EUR");
    }

    public Money times(int qty) {
        return new Money(amount.multiply(BigDecimal.valueOf(qty)), currency);
    }

}