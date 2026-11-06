package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ManyToMany;

@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_seq")
    @SequenceGenerator(
        name = "tag_seq",
        sequenceName = "tag_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    protected Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();

    void addProduct(Product product) { products.add(product); }   // package-private, used by Product.tag()

    public String getName() { return name; }
    public Set<Product> getProducts() { return Collections.unmodifiableSet(products); }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Tag other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Tag.class.hashCode(); }
    @Override public String toString() { return "Tag[id=%d, name=%s]".formatted(id, name); }
}