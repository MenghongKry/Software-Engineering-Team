## Task 5: Pagination, caching and versioning

**Goal:** make the list scalable (pages + `Link` headers), make one product cacheable (`ETag`, 304, `Cache-Control`), protect PUT against lost updates (428 / 412), offer a second representation by media type (v2) and add `/products/{id}/reviews` as a sub-resource.

| Part | File(s) | What it does |
|------|---------|--------------|
| 1 | `ProductFilter`, `Page`, `ProductService.searchPage`, `ProductResource.list` | `page` / `size`, `X-Total-Count`, `Link: first, prev, next, last` |
| 2 | `ETags`, `ProductResource.getById` | `ETag` from the version, 304 for `If-None-Match`, `Cache-Control` |
| 3 | `ETags`, `ProductService.replace`, `StaleVersionException` | PUT needs `If-Match`: 428 missing, 412 stale, 200 + new ETag |
| 4 | `ProductV2`, `ProductResource.getByIdV2` | `application/vnd.shop.v2+json`, v1 stays the default |
| 5 | `Review`, `ReviewRepository`, `ReviewDto`, `ReviewResource`, locator in `ProductResource` | `GET` / `POST /products/{id}/reviews` |
| 6 | `docs/task5-http.md` | Recorded curl transcript |

---

### Part 1 - Pagination

**What I did:**
- `ProductFilter` got two more query parameters: `page` (`@DefaultValue("0") @Min(0)`) and `size` (`@DefaultValue("20") @Min(1) @Max(100)`). They are validated by the same `@Valid @BeanParam` as in Task 3, so `size=500` is a 400 problem+json.
- The repository seeds 30 more products in a loop (`Item 4` .. `Item 33`, SKU `IT-04` .. `IT-33`), so there are 33 products.
- Filtering and paging live in the domain: `ProductService.searchPage(query, page, size)` returns a `Page<Product>` record (items, page, size, total) with `lastPage()`, `hasPrevious()` and `hasNext()`.
- The resource only turns the page into HTTP:

```java
Page<Product> page = service.searchPage(f.toQuery(), f.page, f.size);
var links = new ArrayList<Link>();
links.add(pageLink(uriInfo, 0, "first"));
if (page.hasPrevious()) links.add(pageLink(uriInfo, page.page() - 1, "prev"));
if (page.hasNext())     links.add(pageLink(uriInfo, page.page() + 1, "next"));
links.add(pageLink(uriInfo, page.lastPage(), "last"));
return Response.ok(new GenericEntity<List<ProductDto>>(body) { })
        .links(links.toArray(Link[]::new))
        .header("X-Total-Count", page.total())
        .build();
```

**Why `getRequestUriBuilder().replaceQueryParam("page", n)`:** it copies the URL the client really used, so `q`, `sort`, `size` and the host/port stay the same and only `page` changes. No `localhost:8080` is written anywhere.

**Why `GenericEntity`:** the body is a `List<ProductDto>` inside a `Response`. Because of type erasure Jersey would only see `List`; `GenericEntity` keeps the element type for the JSON/XML writer.

`X-Total-Count` is the number **after** filtering (e.g. `?q=item` gives 30).

### Part 2 - ETag, 304 and Cache-Control

```java
EntityTag tag = ETags.of(p);                                  // "1", "2", ... = version in hex
Response.ResponseBuilder notModified = request.evaluatePreconditions(tag);
if (notModified != null) return notModified.tag(tag).build(); // 304, no body
CacheControl cc = new CacheControl();
cc.setPrivate(true); cc.setMaxAge(60); cc.setNoTransform(true);
return Response.ok(body).tag(tag).cacheControl(cc).build();
```

- The version already increases on every save (Task 1), so it is a perfect ETag.
- `evaluatePreconditions` compares the tag with `If-None-Match` and returns a 304 builder when the client already has this version.
- `private, max-age=60, no-transform`: only the user's browser may cache it, for 60 s, and proxies must not change it (for example recompress).

### Part 3 - Conditional PUT (428 / 412)

- `ETags.expectedVersion(ifMatch)` reads the header: missing gives **428 Precondition Required**, `"2"` or `W/"2"` gives version 2, `*` means any version, anything else can never match (412).
- The real comparison happens in `ProductService.replace(id, data, expectedVersion)`, which is `synchronized`. Checking and saving in one step matters: if two clients both send `If-Match: "1"`, the first one wins and the second gets **412**. Checking in the resource and saving later would let both win.
- On success the answer is 200 with the new `ETag`, so the client can edit again without another GET.
- `PATCH /stock` was not changed and still works without preconditions.

### Part 4 - v2 by media type

- `ProductV2(long id, String name, String sku, Money price, int stock, boolean available)` is a record (JSON only).
- There are now two `GET {id}` methods. Jersey chooses by `Accept`:

| Method | `@Produces` |
|--------|-------------|
| `getById` (v1) | `application/json; qs=1.0`, `application/vnd.shop.v1+json`, `application/xml; qs=0.9` |
| `getByIdV2` | `application/vnd.shop.v2+json; qs=0.5` |

- `qs` is the server's preference. With `Accept: */*` (curl, browsers) both methods could answer, and the higher `qs` wins, so old clients keep getting v1.
- One small fix was needed: `Money` had a method `isEuro()`. JSON-B treats every `isX()` method as a property, so v2 printed an extra `"isEuro":true`. I renamed it to `inCurrency(String)` (a method with a parameter is not a property).

### Part 5 - Reviews sub-resource

```java
@Path("{id: \\d+}/reviews")                  // no @GET/@POST: this is a locator
public ReviewResource reviews(@PathParam("id") long id) {
    return new ReviewResource(service.get(id), reviews);    // 404 here for an unknown product
}
```

- A **sub-resource locator** has `@Path` but no HTTP method. Jersey calls it, then keeps matching the rest of the request inside the returned object.
- Because the locator calls `service.get(id)` first, `/products/999/reviews` is a 404 before `ReviewResource` exists. `ReviewResource` itself never checks the product.
- `ReviewResource` is created per request with two `final` fields, so it is still stateless. It offers `GET` (list) and `POST` (`@Valid @NotNull ReviewDto`: `@Min(1) @Max(5) stars`, `@NotBlank author`, `@Size(max = 500) text`) with 201 + Location from `UriInfo`.
- `ReviewRepository` is a single shared instance (`ConcurrentHashMap` + `AtomicLong`), bound in `Main.config()` like the product repository.

### Check

Full transcript with all headers: `docs/task5-http.md`.

| Request | Result |
|---------|--------|
| `GET /products?page=1&size=10` | 200, `X-Total-Count: 33`, `Link` first/prev/next/last |
| `GET /products?size=500` | 400 problem+json |
| `GET /products/1` | 200, `ETag: "1"`, `Cache-Control: private, no-transform, max-age=60` |
| same with `If-None-Match: "1"` | 304 |
| `PUT /products/1` without `If-Match` | 428 |
| `PUT` with `If-Match: "0"` | 412 |
| `PUT` with `If-Match: "1"` | 200, `ETag: "2"` |
| `Accept: application/vnd.shop.v2+json` | v2 shape with Money and `available` |
| `POST /products/1/reviews` | 201 + Location |
| `GET /products/999/reviews` | 404 problem+json |

### Files changed

- **New:** `domain/Page`, `domain/StaleVersionException`, `domain/Review`, `domain/ReviewRepository`, `api/ETags`, `api/ReviewResource`, `api/dto/ProductV2`, `api/dto/ReviewDto`, `docs/task5-http.md`
- **Changed:** `ProductResource` (paged list, ETag, v2, conditional PUT, locator), `ProductFilter` (`page`, `size`), `ProductService` (`searchPage`, versioned `replace`), `InMemoryProductRepository` (30 more products), `Money` (`isEuro` renamed), `Main` (binds `ReviewRepository`)
