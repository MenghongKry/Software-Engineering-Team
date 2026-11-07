package edu.itc.se.shop_jpa.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.se.shop_jpa.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
}