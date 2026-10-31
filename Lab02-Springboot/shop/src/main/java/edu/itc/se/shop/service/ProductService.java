package edu.itc.se.shop.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public List<Product> findAll(String query) {
        return query == null || query.isBlank()
                ? repo.findAll()
                : repo.findByNameContaining(query);
    }

    public Product findById(long id) {
        return repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(String name, BigDecimal price, int stock) {
        Product product = new Product(null, name, price, stock);
        long id = repo.insert(product);
        log.info("created product {} '{}'", id, name);
        return new Product(id, name, price, stock);
    }

    public Product updateStock(long id, int delta) {
        if (repo.updateStock(id, delta) == 0) {
            if (repo.findById(id).isEmpty()) {
                throw new ProductNotFoundException(id);
            }
            throw new InsufficientStockException(id);
        }
        return findById(id);
    }

    public void delete(long id) {
        if (repo.delete(id) == 0) {
            throw new ProductNotFoundException(id);
        }
    }

    /** Moves qty units of stock from one product to another: both updates happen, or neither. */
    @Transactional
    public void restock(long fromId, long toId, int qty) {
        if (repo.updateStock(fromId, -qty) == 0) {
            if (repo.findById(fromId).isEmpty()) {
                throw new ProductNotFoundException(fromId);
            }
            throw new InsufficientStockException(fromId);
        }
        if (repo.updateStock(toId, qty) == 0) {
            throw new ProductNotFoundException(toId);
        }
    }
}
