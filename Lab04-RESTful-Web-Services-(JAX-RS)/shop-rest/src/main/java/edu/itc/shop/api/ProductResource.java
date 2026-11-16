package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.domain.DuplicateSkuException;
import edu.itc.shop.domain.InsufficientStockException;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductRepository repo;

    @Inject
    public ProductResource(ProductRepository repo) {
        this.repo = repo;
    }

    // GET /api/products
    @GET
    public List<ProductDto> list() {
        return repo.findAll().stream().map(ProductDto::from).toList();
    }

    // GET /api/products/{id}
    @GET
    @Path("{id: \\d+}")
    public ProductDto getById(@PathParam("id") long id) {
        return ProductDto.from(find(id));
    }

    // POST /api/products -> 201 + Location, 409 if SKU exists
    @POST
    public Response create(ProductDto dto, @Context UriInfo uriInfo) {
        repo.findBySku(dto.sku).ifPresent(p -> { throw new DuplicateSkuException(dto.sku); });
        Product saved = repo.save(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
        return Response.created(location).entity(ProductDto.from(saved)).build();
    }

    // PUT /api/products/{id} -> 200, 404, 409
    @PUT
    @Path("{id: \\d+}")
    public ProductDto replace(@PathParam("id") long id, ProductDto dto) {
        Product existing = find(id);
        repo.findBySku(dto.sku).ifPresent(p -> {
            if (p.id() != id) throw new DuplicateSkuException(dto.sku);
        });
        Product saved = repo.save(dto.toProduct(id, existing.version()));
        return ProductDto.from(saved);
    }

    // DELETE /api/products/{id} -> 204, 404
    @DELETE
    @Path("{id: \\d+}")
    public Response delete(@PathParam("id") long id) {
        find(id);
        repo.delete(id);
        return Response.noContent().build();
    }

    // PATCH /api/products/{id}/stock -> 200, 404, 409
    @PATCH
    @Path("{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDto changeStock(@PathParam("id") long id, StockChange change) {
        Product p = find(id);
        int newStock = p.stock() + change.delta();
        if (newStock < 0) throw new InsufficientStockException(p.stock(), change.delta());
        Product saved = repo.save(new Product(p.id(), p.name(), p.sku(), p.price(), newStock, p.version()));
        return ProductDto.from(saved);
    }

    private Product find(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}