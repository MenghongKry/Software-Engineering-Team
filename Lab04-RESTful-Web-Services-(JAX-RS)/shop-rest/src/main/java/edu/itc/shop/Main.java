
package edu.itc.shop;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import edu.itc.shop.api.IdempotencyStore;
import edu.itc.shop.domain.InMemoryProductRepository;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.ProductService;
import edu.itc.shop.domain.ReviewRepository;

public class Main {

    public static final URI BASE =
            URI.create("http://localhost:8080/api/");

    /**
     * The application: scanned resources/providers + shared singletons.
     * Every call builds fresh singletons, so each JerseyTest gets its own data.
     */
    public static ResourceConfig config() {
        return new ResourceConfig()
                .packages("edu.itc.shop.api")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        ProductRepository repo = new InMemoryProductRepository();
                        bind(repo).to(ProductRepository.class);
                        ProductService products = new ProductService(repo);
                        bind(products).to(ProductService.class);
                        bind(new ReviewRepository()).to(ReviewRepository.class);
                        bind(new OrderService(products)).to(OrderService.class);
                        bind(new IdempotencyStore()).to(IdempotencyStore.class);
                    }
                });
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE, config());
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        System.out.println("shop-rest listening on " + BASE);
        Thread.currentThread().join();
    }
}
