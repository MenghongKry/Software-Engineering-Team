package edu.itc.se.shop_jpa.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(
        name = "orders_seq",
        sequenceName = "orders_seq",
        allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {}

    public Order(Customer customer) {
        this.customer = customer;
        this.status = OrderStatus.NEW;
        this.placedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
}