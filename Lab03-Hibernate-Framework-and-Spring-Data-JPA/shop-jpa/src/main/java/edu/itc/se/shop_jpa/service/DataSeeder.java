package edu.itc.se.shop_jpa.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop_jpa.domain.*;
import edu.itc.se.shop_jpa.repository.*;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final TagRepository tags;
    private final OrderRepository orders;

    public DataSeeder(CustomerRepository customers, ProductRepository products,
                      TagRepository tags, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.tags = tags;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) return;              // idempotent

        // --- tags ---
        Tag sale = tags.save(new Tag("sale"));
        Tag fresh = tags.save(new Tag("new"));
        Tag gift = tags.save(new Tag("gift"));

        // --- 3 customers ---
        List<Customer> cs = customers.saveAll(List.of(
                customer("Ada Lovelace",  "ada@example.com",   "London"),
                customer("Grace Hopper",  "grace@example.com", "New York"),
                customer("Alan Turing",   "alan@example.com",  "Manchester"),
                customer("Menghong Kry",  "menghong@example.com", "Phnom Penh")));

        // --- 12 products ---
        List<Product> ps = products.saveAll(List.of(
                product("KB-1", "Keyboard basic",      "39.00",  Category.HARDWARE, 10, sale),
                product("KB-7", "Mechanical keyboard", "59.00",  Category.HARDWARE, 4,  sale, fresh),
                product("MS-2", "Wireless mouse",      "19.90",  Category.HARDWARE, 0),
                product("MN-3", "27\" monitor",        "189.00", Category.HARDWARE, 3,  fresh),
                product("BK-1", "Clean Code",          "34.50",  Category.BOOKS,    12, gift),
                product("BK-2", "Effective Java",      "45.00",  Category.BOOKS,    2,  gift, sale),
                product("BK-3", "Refactoring",         "42.00",  Category.BOOKS,    0),
                product("TY-1", "Puzzle 1000 pieces",  "14.99",  Category.TOYS,     25, gift),
                product("TY-2", "Robot kit",           "79.00",  Category.TOYS,     5,  fresh, gift),
                product("TY-3", "Board game",          "29.00",  Category.TOYS,     1,  sale),
                product("FD-1", "Coffee beans",        "12.50",  Category.FOOD,     40),
                product("FD-2", "Dark chocolate",      "3.20",   Category.FOOD,     100, gift)));

        // --- 30 orders, 2–4 lines each, mixed statuses ---
        Random rnd = new Random(42);                    // fixed seed → same data every run
        OrderStatus[] statuses = OrderStatus.values();
        Instant now = Instant.now();

        for (int i = 0; i < 30; i++) {
            Order order = new Order(cs.get(i % cs.size()), now.minus(i * 7L, ChronoUnit.HOURS));
            order.setStatus(statuses[i % statuses.length]);

            List<Product> shuffled = new ArrayList<>(ps);
            Collections.shuffle(shuffled, rnd);
            int lineCount = 2 + rnd.nextInt(3);         // 2..4
            for (int j = 0; j < lineCount; j++) {
                order.addLine(shuffled.get(j), 1 + rnd.nextInt(3));
            }
            orders.save(order);                         // cascades to the lines
        }
    }

    private static Customer customer(String name, String email, String city) {
        Address a = new Address("1 Main St", city, "10000");
        return new Customer(name, email, a, a);
    }

    private static Product product(String sku, String name, String price,
                                   Category category, int stock, Tag... tagList) {
        Product p = new Product(sku, name, Money.eur(price), category);
        p.setStock(stock);
        for (Tag t : tagList) p.tag(t);
        return p;
    }
}