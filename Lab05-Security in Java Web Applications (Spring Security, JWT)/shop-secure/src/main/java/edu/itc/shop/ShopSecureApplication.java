
package edu.itc.shop;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;

@SpringBootApplication
public class ShopSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopSecureApplication.class, args);
    }

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {

        return args -> {

            repository.save(new Product(
                    1,
                    "USB-C cable",
                    new BigDecimal("7.90"),
                    25
            ));

            repository.save(new Product(
                    2,
                    "Keyboard",
                    new BigDecimal("35.00"),
                    10
            ));

            repository.save(new Product(
                    3,
                    "Mouse",
                    new BigDecimal("19.90"),
                    15
            ));
        };
    }
}
