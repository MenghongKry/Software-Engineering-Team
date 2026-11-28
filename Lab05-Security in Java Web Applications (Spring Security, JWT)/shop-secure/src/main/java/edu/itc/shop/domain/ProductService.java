
package edu.itc.shop.domain;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Product not found: " + id
                        ));
    }

    public Product create(Product product) {
        return repository.save(product);
    }

    public void delete(long id) {
        findById(id);
        repository.deleteById(id);
    }
}
