package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(
        name = "customer_seq",
        sequenceName = "customer_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Embedded
    private Address billing;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
            name = "street",
            column = @Column(name = "ship_street", nullable = false, length = 120)
        ),
        @AttributeOverride(
            name = "city",
            column = @Column(name = "ship_city", nullable = false, length = 80)
        ),
        @AttributeOverride(
            name = "zip",
            column = @Column(name = "ship_zip", nullable = false, length = 12)
        )
    })
    private Address shipping;

    protected Customer() {}

    public Customer(String name, String email, Address billing, Address shipping) {
        this.name = name;
        this.email = email;
        this.billing = billing;
        this.shipping = shipping;
    }

    public Long getId() {
        return id;
    }
}