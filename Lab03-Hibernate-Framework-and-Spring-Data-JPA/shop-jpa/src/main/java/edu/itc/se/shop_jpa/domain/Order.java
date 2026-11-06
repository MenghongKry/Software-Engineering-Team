package edu.itc.se.shop_jpa.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

    public OrderLine addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(product, quantity, product.getPrice());
        lines.add(line);
        line.setOrder(this);
        return line;
    }

    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }
    public List<OrderLine> getLines() {                  // the getter, add it
        return Collections.unmodifiableList(lines);
    }
    public Money total() {
        return lines.stream()
                .map(l -> l.getUnitPrice().times(l.getQuantity()))
                .reduce(Money.eur("0"),
                        (a, b) -> new Money(a.amount().add(b.amount()), a.currency()));
    }

    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public Instant getPlacedAt() { return placedAt; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Order other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Order.class.hashCode(); }
    @Override public String toString() { return "Order[id=%d, status=%s]".formatted(id, status); }

}