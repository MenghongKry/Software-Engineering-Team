package edu.itc.shop;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.Role;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AccountService;

@SpringBootApplication
public class ShopSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSecureApplication.class, args);
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            repository.save(new Product(1, "USB-C cable", new BigDecimal("7.90"), 25));
            repository.save(new Product(2, "Keyboard", new BigDecimal("35.00"), 10));
            repository.save(new Product(3, "Mouse", new BigDecimal("19.90"), 15));
        };
    }

    // Task 2: seed users (idempotent, passwords come from application.yml)
    @Bean
    CommandLineRunner seedUsers(AccountService accounts,
                                UserAccountRepository repo,
                                @Value("${shop.seed.admin-password:Admin-Dev-Pass-1}") String adminPw,
                                @Value("${shop.seed.customer-password:Alice-Dev-Pass-1}") String customerPw) {
        return args -> {
            if (!repo.existsByUsername("admin")) {
                accounts.register("admin", adminPw, Set.of(Role.ADMIN, Role.CUSTOMER));
            }
            if (!repo.existsByUsername("alice")) {
                accounts.register("alice", customerPw, Set.of(Role.CUSTOMER));
            }
        };
    }

    // TEMPORARY: prints users to the log for security-session.md. DELETE after copying one row.
    @Bean
    CommandLineRunner printUsers(UserAccountRepository repo) {
        return args -> repo.findAll().forEach(u ->
                System.out.println("USER ROW: " + u.getId() + " | " + u.getUsername()
                        + " | " + u.getPasswordHash() + " | " + u.isEnabled()));
    }
}