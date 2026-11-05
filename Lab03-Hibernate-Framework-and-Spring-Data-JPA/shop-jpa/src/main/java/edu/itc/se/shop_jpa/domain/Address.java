package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
    @Column(name = "street", nullable = false, length = 120)
    String street,

    @Column(name = "city", nullable = false, length = 80)
    String city,

    @Column(name = "zip", nullable = false, length = 12)
    String zip
) {}