## Task 2: Full CRUD with status codes, headers, JSON and XML

### What I built
I finished `ProductResource` so it now supports create, update, delete and a stock change. Every endpoint returns a `ProductDto` instead of the `Product` record, so the `version` field is not shown to the client anymore. The same DTO works for both JSON and XML.

### Endpoints

| Method | URL | Success | Errors |
|--------|-----|---------|--------|
| GET | `/api/products` | 200 | - |
| GET | `/api/products/{id}` | 200 | 404 |
| POST | `/api/products` | 201 + `Location` header | 409 (duplicate SKU), 415 (wrong content type) |
| PUT | `/api/products/{id}` | 200 | 404, 409 (SKU used by another product) |
| DELETE | `/api/products/{id}` | 204 | 404 |
| PATCH | `/api/products/{id}/stock` | 200 | 404, 409 (stock would go negative) |

### How it works
- **JSON and XML:** the resource class has `@Produces` and `@Consumes` for both. Jersey picks the format from the `Accept` and `Content-Type` headers. `ProductDto` is a normal class with JAXB annotations (not a record), because JAXB needs a no-arg constructor.
- **Location header:** POST builds the URL with `UriInfo`, so I don't hardcode `localhost:8080`.
- **Business rules:** the duplicate SKU and negative stock checks throw `DuplicateSkuException` and `InsufficientStockException` (in the `domain` package). Both return 409. The resource only throws them and doesn't build error responses itself.
- **415:** I didn't write any code for this. Jersey returns it by itself when the body is `text/plain`.
- **PATCH:** it only accepts JSON, with a body like `{"delta": -5}`. It uses the `StockChange` record.

### What was tricky
- I almost forgot that PUT also needs to check the SKU. If the SKU belongs to the *same* product it is fine, but if it belongs to another one it should be 409.
- It was easy to mix up which methods need `find(id)`. DELETE calls it only to get the 404 before deleting.
- The resource still has no mutable fields. The repository is injected once and shared, as in Task 1.

### Testing
I ran the server with `mvn -q compile exec:java` and tested with curl. The full transcript (all eight calls with headers) is in `docs/task2-http.md`.