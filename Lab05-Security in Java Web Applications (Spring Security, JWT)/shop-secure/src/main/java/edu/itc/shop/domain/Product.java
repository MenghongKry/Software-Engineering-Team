
package edu.itc.shop.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Product(
        long id,

        @NotBlank
        String name,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal price,

        @Min(0)
        int stock
) {
}
