package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(
        name = "product_seq",
        sequenceName = "product_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Embedded
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private int stock;

    /** Challenge: optimistic locking for the stock reservation (column added by V3). */
    @Version
    @Column(nullable = false)
    private long version;

    protected Product() {}

    public Product(String sku, String name, Money price, Category category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = 0;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public long getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stock = stock;
    }

    @ManyToMany
    @JoinTable(
            name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    public Set<Tag> getTags() { return Collections.unmodifiableSet(tags); }

    public void tag(Tag tag) {
        tags.add(tag);
        tag.addProduct(this);     // keep both sides in sync
    }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Product other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Product.class.hashCode(); }
    @Override public String toString() {
        return "Product[id=%d, sku=%s, name=%s]".formatted(id, sku, name);
    }
}