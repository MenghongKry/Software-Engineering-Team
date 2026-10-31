package edu.itc.se.shop.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {
    private final JdbcClient jdbc;

    public CustomerRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public boolean exists(long id) {
        return jdbc.sql("SELECT COUNT(*) FROM customer WHERE id = :id")
                .param("id", id)
                .query(Long.class).single() > 0;
    }
}
