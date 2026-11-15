package edu.itc.shop.api;

import java.util.List;

import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductRepository repo;

    @Inject
    public ProductResource(ProductRepository repo) {
        this.repo = repo;
    }

    // GET /api/products
    @GET
    public List<Product> list() {
        return repo.findAll();
    }

    // GET /api/products/{id}
    @GET
    @Path("{id: \\d+}")
    public Product getById(@PathParam("id") long id) {

        return repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Product " + id + " not found"
                        )
                );
    }
}
