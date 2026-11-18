## Challenge: orders with HATEOAS, idempotent POST, OpenAPI and a full test suite

All parts of the challenge are done:

| Part | File(s) |
|------|---------|
| Orders sub-API | `domain/Order`, `OrderLine`, `OrderStatus`, `OrderService`, `OrderNotFoundException`, `OrderStateException`, `ProductService.reserve/release`, `api/OrderResource`, `api/dto/PlaceOrder`, `api/dto/OrderDto` |
| HATEOAS | `OrderResource.represent` + `withLinks` |
| Idempotency | `api/IdempotencyStore`, `api/Hashes`, `api/HttpErrors` |
| OpenAPI | `pom.xml` (API + plugin), annotations on all resources, `api/OpenApiResource`, `api/ApiDocs` |
| Tests | `src/test/java/edu/itc/shop/ShopApiTest.java` (23 tests) |
| JAX-RS vs Spring MVC | section in `README.md` |
| Transcript | `docs/challenge-http.md` |

---

### Orders

| Method | URL | Success | Errors |
|--------|-----|---------|--------|
| POST | `/api/orders` (needs `Idempotency-Key`) | 201 + `Location` | 400, 401, 404 unknown product, 409 not enough stock, 415, 422, 428 |
| GET | `/api/orders/{id}` | 200 | 404 |
| PUT | `/api/orders/{id}/payment` | 200, NEW -> PAID | 401, 404, 409 when not NEW |
| DELETE | `/api/orders/{id}` | 200, NEW -> CANCELLED, stock is given back | 401, 404, 409 when PAID or already CANCELLED |

- Body of POST: `PlaceOrder(List<Line> lines)` with `Line(@Positive long productId, @Min(1) int quantity)`, `@NotEmpty` list, validated like every other body.
- **All rules are in the domain.** `OrderService` decides the transitions and throws `OrderStateException` (409) or `OrderNotFoundException` (404). The resource only calls it.
- **Stock is all or nothing.** `ProductService.reserve(lines)` (synchronized, same lock as the other product writes) first checks that every product exists and has enough stock, and only then takes the stock. So an order with one bad line changes nothing. It also returns the total (`price * quantity` summed).
- DELETE answers 200 with the cancelled order (not 204), because the order still exists, just with status CANCELLED, and the client should see that.
- Order ids are `o-1001`, `o-1002`, ... from an `AtomicLong`.

### HATEOAS

Every order answer (POST, replay, GET, pay, cancel) carries the same links twice: as `Link` headers and in a `_links` map in the body.

| rel | When | Target |
|-----|------|--------|
| `self` | always | `GET /orders/{id}` |
| `pay` | only while NEW | `PUT /orders/{id}/payment` |
| `cancel` | only while NEW | `DELETE /orders/{id}` |
| `product-<id>` | one per line | `GET /products/<id>` |

All URIs come from `uriInfo.getBaseUriBuilder().path(OrderResource.class)...` (and `ProductResource.class` for products), never from a fixed host. In the tests the server runs on port 9998 and the links follow automatically.

A client can do the whole flow from the POST answer: follow `pay` to pay, or `cancel` to cancel. After paying, `pay` and `cancel` disappear, which tells the client that these steps are no longer allowed. The test `hateoas_placeAndPayByFollowingLinksOnly` does exactly that and never builds a URL itself.

### Idempotency

```java
if (key == null || key.isBlank()) throw HttpErrors.preconditionRequired(...);      // 428
String hash = Hashes.sha256(cmd);
var result = idempotency.once(key, () -> {                                           // computeIfAbsent
    Order order = orders.place(cmd.toLines());                                       // 404 / 409 inside
    return new Stored(hash, 201, orderUri(uriInfo, order.id()), represent(order, uriInfo));
});
if (!result.stored().bodyHash().equals(hash)) throw HttpErrors.unprocessable(...);  // 422
return ... .header("Idempotency-Replayed", result.replayed()) ...
```

- `IdempotencyStore` is one shared instance (bound in `Main.config()`) with a `ConcurrentHashMap<String, Stored>`.
- **Race-free:** `computeIfAbsent` runs the lambda at most once per key. If two retries arrive at the same moment, the second one waits until the first has finished and then gets the stored response. The test `idempotency_concurrentRetriesCreateOneOrder` sends 10 parallel POSTs with the same key: exactly one says `Idempotency-Replayed: false`, all have the same Location, and the stock is reduced once.
- **Body hash:** SHA-256 of the parsed `PlaceOrder` record (`toString()` of a record is stable), so extra spaces in the JSON do not matter, but a different order with the same key is a 422.
- If placing fails (404 / 409), the exception leaves `computeIfAbsent` and nothing is stored, so the client can fix the problem and retry with the same key.
- Jakarta REST 4.0 has no `Response.Status` constant for 422, so `HttpErrors.unprocessable` builds the status with its reason phrase (`Response.status(422, "Unprocessable Content")`). `ProblemMapper` now copies the full status info, so the status line also reads `422 Unprocessable Content`.

### OpenAPI

- `org.eclipse.microprofile.openapi:microprofile-openapi-api:4.1` with scope `provided`: the annotations are only needed while building.
- `io.smallrye:smallrye-open-api-maven-plugin` (goal `generate-schema`, phase `process-classes`) scans `edu.itc.shop` after compiling and writes `target/classes/META-INF/openapi.yaml` (OpenAPI 3.1.0). It is configured with `enableStandardStaticFiles=false`, otherwise the next build would read the previous `META-INF/openapi.yaml` back in as input.
- `ProductResource`, `ReviewResource`, `OrderResource` and `OpenApiResource` have `@Tag`, `@Operation`, `@Parameter` and one `@APIResponse` for **every** status code the method can return (errors point to the `Problem` schema with `application/problem+json`).
- `OpenApiResource` serves the file at `GET /api/openapi` with `Content-Type: application/yaml`.

### Tests

`ShopApiTest extends JerseyTest`, and `configure()` returns `Main.config()`, so the tests run the real application with all filters, mappers and providers. `config()` builds new singletons each time, and JerseyTest calls it before every test, so every test starts with fresh data (33 products, no orders, no keys).

| Status | Test |
|--------|------|
| 200 | paged list with `Link` + `X-Total-Count`; v1/v2; PUT with matching `If-Match` + new `ETag` |
| 201 | create product (`Location`); review via sub-resource; same order twice (`Idempotency-Replayed`) |
| 204 | delete product |
| 304 | `If-None-Match` with current `ETag` |
| 400 | invalid body (4 errors); `size=500` |
| 401 | write without `X-Api-Key` (`WWW-Authenticate`) |
| 404 | unknown product; reviews of unknown product |
| 406 | `Accept: text/csv` |
| 409 | duplicate SKU; order with not enough stock |
| 412 | PUT with stale `If-Match` |
| 415 | `text/plain` body |
| 422 | same `Idempotency-Key`, different body |
| 428 | PUT without `If-Match`; POST /orders without `Idempotency-Key` |

Plus: HATEOAS place + pay by following links (and 409 for paying twice / cancelling a paid order), cancel restores stock, 10 concurrent retries create one order, and `/openapi` is served as YAML. Every error test also checks `Content-Type: application/problem+json` and the `status`, `title` and `instance` fields.

```text
$ mvn -q verify
$ cat target/surefire-reports/edu.itc.shop.ShopApiTest.txt
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

JerseyTest 4 already has JUnit 5 `@BeforeEach` / `@AfterEach` on `setUp()` / `tearDown()`, so no extra code was needed to start and stop the test server.
