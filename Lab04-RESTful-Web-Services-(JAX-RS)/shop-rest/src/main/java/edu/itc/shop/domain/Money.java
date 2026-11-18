package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Money(BigDecimal amount, String currency) {

    // digits, optional decimals (max 2), one space, three capital letters
    private static final Pattern FORMAT = Pattern.compile("(\\d+(?:\\.\\d{1,2})?) ([A-Z]{3})");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
    }

    public static Money parse(String text) {
        if (text == null) throw new IllegalArgumentException("Money text is null");
        Matcher m = FORMAT.matcher(text.trim());
        if (!m.matches()) throw new IllegalArgumentException("Cannot parse Money from '" + text + "'");
        return new Money(new BigDecimal(m.group(1)), m.group(2));
    }

    /** Not named isEuro(): JSON-B would turn an isX() method into an extra "isEuro" property. */
    public boolean inCurrency(String code) {
        return currency.equals(code);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}