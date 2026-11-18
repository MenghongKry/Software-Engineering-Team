# shop-rest

A product catalogue and orders API built with **Jakarta REST 4.0** on **Eclipse Jersey 4** and an embedded **Grizzly** server (Lab-04). JDK 25, Maven 3.9+.

Done: Tasks 1-5 and the whole challenge (orders with HATEOAS, idempotent POST, OpenAPI, JerseyTest suite, JAX-RS vs Spring MVC).

## How to run

```bash
mvn -q compile exec:java
```

The server prints `shop-rest listening on http://localhost:8080/api/`. Stop it with `Ctrl+C`. All data is in memory, so a restart resets it (33 products, no reviews, no orders).

Writes (`POST`, `PUT`, `PATCH`, `DELETE`) need the header `X-Api-Key`. The default key is `demo-123`; other keys can be given at start:

```bash
mvn -q compile exec:java -Dshop.api.keys=key1,key2
```

## How to test

```bash
mvn -q verify
```

This compiles, generates the OpenAPI document and runs `ShopApiTest` (23 JerseyTests, one per status code plus the order flows). The result is in `target/surefire-reports/`.

By hand: every task has a curl transcript in `docs/` that can be replayed against a freshly started server. A Thunder Client collection for Task 4 is in `test-requests/`.

## Endpoints

| Method | URL | Notes |
|--------|-----|-------|
| GET | `/api/products` | `q`, `minPrice`, `maxPrice` (`"5 EUR"`), `inStock`, `sort=field,dir`, `page`, `size`; `Link` + `X-Total-Count`; gzip |
| GET | `/api/products/{id}` | JSON (v1), XML, or `application/vnd.shop.v2+json`; `ETag`, 304, `Cache-Control` |
| POST | `/api/products` | 201 + `Location` |
| PUT | `/api/products/{id}` | needs `If-Match` (428 / 412) |
| PATCH | `/api/products/{id}/stock` | `{"delta": -5}` |
| DELETE | `/api/products/{id}` | 204 |
| GET, POST | `/api/products/{id}/reviews` | sub-resource |
| POST | `/api/orders` | needs `Idempotency-Key` |
| GET | `/api/orders/{id}` | links: `self`, `pay`, `cancel`, `product-N` |
| PUT | `/api/orders/{id}/payment` | NEW -> PAID |
| DELETE | `/api/orders/{id}` | NEW -> CANCELLED |
| GET | `/api/openapi` | OpenAPI 3.1 (YAML) |

Every error is `application/problem+json` (RFC 9457) with `type`, `title`, `status`, `detail` and `instance`.

## Project layout

```text
src/main/java/edu/itc/shop/
├── Main.java          Grizzly bootstrap + config() (package scan + shared singletons)
├── domain/            Product, Money, Order, Review, repositories, services, domain exceptions (no HTTP code)
└── api/
    ├── ProductResource, ReviewResource, OrderResource, OpenApiResource
    ├── dto/           ProductDto, ProductV2, ReviewDto, PlaceOrder, OrderDto, StockChange, Problem
    ├── params/        ProductFilter (@BeanParam), MoneyConverterProvider
    ├── error/         ValidationMapper, ProblemMapper
    └── filter/        CorrelationFilter, CorsFilter, ApiKeyFilter, Compress + GzipWriterInterceptor
src/test/java/edu/itc/shop/ShopApiTest.java
docs/task1-http.md ... task5-http.md, challenge-http.md
```

**Why the resources have no mutable fields:** Jersey creates a new resource object per request, but the objects they use (repositories, services, the idempotency store) are single shared instances used by many request threads at once, so a resource only keeps `final` references to them and all shared state lives in thread-safe classes (`ConcurrentHashMap`, `AtomicLong`, `synchronized` service methods).

## Write-ups per task

- [Task 1 - First resource and server](README-Task1.md)
- [Task 2 - Full CRUD, status codes, JSON and XML](README-Task2.md)
- Task 3 - Parameters and validation: see `docs/task3-http.md`
- [Task 4 - Providers and filters](README-Task4.md)
- [Task 5 - Pagination, caching and versioning](README-Task5.md)
- [Challenge - Orders, idempotency, OpenAPI, tests](README-Challenge.md)

---

## JAX-RS vs Spring MVC

The same two endpoints, written with Jakarta REST (this lab) and with Spring MVC (Lab-02, `edu.itc.se.shop.web.ProductController`).

### GET /products/{id}

Jakarta REST (Lab-04):

```java
@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {
    private final ProductService service;

    @Inject
    public ProductResource(ProductService service, ReviewRepository reviews) { ... }

    @GET
    @Path("{id: \\d+}")
    public Response getById(@PathParam("id") long id, @Context Request request) {
        Product p = service.get(id);                      // throws ProductNotFoundException -> 404
        return cacheable(p, ProductDto.from(p), request); // ETag / 304 / Cache-Control
    }
}
```

Spring MVC (Lab-02):

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable long id) {
        return ResponseEntity.ok(ProductDto.from(service.findById(id)));
    }
}
```

### POST /products

Jakarta REST (Lab-04):

```java
@POST
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
    Product saved = service.create(dto.toProduct(0, 0));      // DuplicateSkuException -> 409
    URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
    return Response.created(location).entity(ProductDto.from(saved)).tag(ETags.of(saved)).build();
}
```

Spring MVC (Lab-02):

```java
@PostMapping
public ResponseEntity<ProductDto> create(@Valid @RequestBody CreateProduct request) {
    Product created = service.create(request.name(), request.price(), request.stock());
    return ResponseEntity
        .created(URI.create("/api/products/" + created.id()))   // Spring would use ServletUriComponentsBuilder
        .body(ProductDto.from(created));
}
```

### Eight differences

| # | Topic | Jakarta REST (Jersey) | Spring MVC |
|---|-------|-----------------------|------------|
| 1 | Annotations | `@Path` on class and method, one annotation per verb (`@GET`, `@POST`, ...), `@PathParam`, `@QueryParam`, `@HeaderParam`, `@BeanParam`; media types with `@Produces` / `@Consumes` | `@RestController` + `@RequestMapping`, combined `@GetMapping("/{id}")`, `@PathVariable`, `@RequestParam`, `@RequestHeader`; media types with `produces =` / `consumes =` |
| 2 | Body binding | The one parameter **without** an annotation is the body; `@Valid` triggers Bean Validation | The body needs `@RequestBody` explicitly; `@Valid` / `@Validated` |
| 3 | Building the response | Return a DTO, or `Response.ok(...)/created(uri)/status(...)` with `.tag()`, `.links()`, `.cacheControl()`; URIs from injected `UriInfo`; `Request.evaluatePreconditions` for 304/412 | Return a DTO, or `ResponseEntity.ok()/created()/status()` with `.eTag()`, `.cacheControl()`; URIs from `ServletUriComponentsBuilder`; `WebRequest.checkNotModified` for 304 |
| 4 | Errors | Throw `WebApplicationException` subclasses (`NotFoundException`, `ClientErrorException(…, 409)`); turn them into bodies with `ExceptionMapper<T>` providers (`ProblemMapper`, `ValidationMapper`) | Throw `ResponseStatusException` or own exceptions; handle them with `@ExceptionHandler` in a `@RestControllerAdvice`; built-in `ProblemDetail` for RFC 9457 |
| 5 | Cross-cutting code | `ContainerRequestFilter` / `ContainerResponseFilter` (`@PreMatching`, `@Priority`), `WriterInterceptor`, `@NameBinding` to attach a filter to chosen methods | Servlet `Filter` / `OncePerRequestFilter`, `HandlerInterceptor`, `ResponseBodyAdvice`; Spring Security for API keys; compression is a property (`server.compression.enabled`) |
| 6 | JSON library | JSON-B (Yasson) via `jersey-media-json-binding`, JAXB for XML; customised with `@JsonbProperty`, `@JsonbPropertyOrder` | Jackson by default (`spring-boot-starter-web`), customised with `@JsonProperty`, `ObjectMapper` settings; XML needs `jackson-dataformat-xml` |
| 7 | Dependency injection | HK2 inside Jersey: `@Inject` constructor, singletons registered by hand in an `AbstractBinder` (`Main.config()`); resources are per request by default | Spring container: constructor injection without annotation, beans found by `@Service` / `@Repository` / `@Component` scanning; controllers are singletons |
| 8 | Testing | `JerseyTest`: starts the real `ResourceConfig` on a test Grizzly server, calls it with the JAX-RS `Client` (`target("products").request().get()`) | `@WebMvcTest` + `MockMvc` (no real server, mocked services with `@MockitoBean`) or `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` / `RestClient` |

In short: Jakarta REST is a small standard API where you assemble the pieces yourself (server, DI bindings, JSON provider), while Spring MVC is one part of a larger framework that wires these pieces automatically through auto-configuration.
