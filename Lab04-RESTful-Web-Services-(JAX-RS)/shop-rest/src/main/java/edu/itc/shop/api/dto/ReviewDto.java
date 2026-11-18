package edu.itc.shop.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import edu.itc.shop.domain.Review;

/** Review representation. id and productId are ignored on input (set by the server). */
public record ReviewDto(long id,
                        long productId,
                        @Min(1) @Max(5) int stars,
                        @NotBlank @Size(max = 80) String author,
                        @Size(max = 500) String text) {

    public static ReviewDto from(Review r) {
        return new ReviewDto(r.id(), r.productId(), r.stars(), r.author(), r.text());
    }
}
