## Task 4: Providers and filters

**Goal:** add behaviour that every request needs (correlation id, access log, CORS, API key, gzip) **without changing the resource**. Each feature is a JAX-RS *provider* in the new package `api/filter`. Jersey finds them automatically through package scanning, because they have `@Provider`, so `Main` did not change.

The only change to `ProductResource` is one annotation, `@Compress`, on the list method (Part 4).

| Part | File(s) | What it does |
|------|---------|--------------|
| 1 | `CorrelationFilter` | Correlation id + one access-log line per request |
| 2 | `CorsFilter` | CORS headers + answers `OPTIONS` preflights |
| 3 | `ApiKeyFilter` | Writes need a valid `X-Api-Key` (401 otherwise) |
| 4 | `Compress`, `GzipWriterInterceptor` | Gzip for `GET /products` when the client accepts it |
| 5 | - | Check the order the filters run in |
| 6 | `docs/task4-http.md`, `test-requests/` | Recorded HTTP transcript + Thunder Client tests |

---

### Part 1 - Correlation id and access log (`CorrelationFilter`)

**Task:** take `X-Correlation-Id` from the request or generate a UUID, return it in the response, and log one line `METHOD path -> status [id] ms`.

**What I did:** one class that is both a `ContainerRequestFilter` and a `ContainerResponseFilter`, with `@Priority(Priorities.USER)`.

**How it works:**
- **Request side:** reads the header (or makes a new UUID) and saves the id and the start time with `setProperty`, so the response side can read them later.
- **Response side:** reads the id and start time back, adds the `X-Correlation-Id` header and logs the line with `System.Logger`.

```java
public void filter(ContainerRequestContext request) {
    request.setProperty(ID_PROPERTY, idOf(request));          // header value or new UUID
    request.setProperty(START_PROPERTY, System.nanoTime());
}

public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    Object id = request.getProperty(ID_PROPERTY);
    if (id == null) id = idOf(request);                       // request was aborted earlier (see below)
    ...
    response.getHeaders().putSingle(HEADER, id);
    LOG.log(Level.INFO, method + " " + path + " -> " + status + " [" + id + "] " + ms + " ms");
}
```

**Why the `if (id == null)`:** when `ApiKeyFilter` or the CORS preflight stops a request early (`abortWith`), the request side of this filter never runs. The response side still runs, so it creates the id itself. That way even a 401 has an `X-Correlation-Id`. These aborted requests show `0 ms` in the log because no start time was saved.

**Check:** send `GET /api/products/1` with header `X-Correlation-Id: order-check-42`. The response has `X-Correlation-Id: order-check-42`, and the server prints:
```text
INFO: GET /api/products/1 -> 200 [order-check-42] 0 ms
```

---

### Part 2 - CORS (`CorsFilter`)

**Task:** add the CORS headers to every response, and answer `OPTIONS` preflights before they reach a resource.

**What I did:** one class with two jobs:
- **Response filter:** puts these headers on every response:
  - `Access-Control-Allow-Origin: http://localhost:5173`
  - `Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE`
  - `Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match`
  - `Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id`
- **`@PreMatching` request filter:** if the method is `OPTIONS`, stop right away with 200.

```java
public void filter(ContainerRequestContext request) {
    if ("OPTIONS".equals(request.getMethod())) {
        request.abortWith(Response.ok().build());   // preflight never reaches a resource
    }
}
```

**Why `@PreMatching`:** a normal filter runs *after* Jersey has picked the resource method. A pre-matching filter runs *before*, so the preflight is answered without Jersey looking for a method at all.

**Check:** `OPTIONS /api/products` with `Origin: http://localhost:5173` and `Access-Control-Request-Method: POST` gives **200** with all the `Access-Control-*` headers.

---

### Part 3 - API key (`ApiKeyFilter`)

**Task:** every method except `GET`, `HEAD` and `OPTIONS` needs a valid `X-Api-Key`. Otherwise answer **401** with a problem+json body and `WWW-Authenticate: ApiKey realm="shop"`. The filter must run **before matching**.

**What I did:** a `@PreMatching` request filter with `@Priority(Priorities.AUTHENTICATION)`.
- Valid keys come from the system property `shop.api.keys` (comma-separated). The default is `demo-123`, so no key is hard-coded anywhere else.
- Reads stay public, and only writes are checked.
- The error body uses the same `Problem` record as Task 3, so all errors look the same.

```java
if (SAFE.contains(ctx.getMethod())) return;                 // GET, HEAD, OPTIONS
String key = ctx.getHeaderString("X-Api-Key");
if (key != null && keys.contains(key)) return;              // valid key -> continue
ctx.abortWith(Response.status(401)
        .header("WWW-Authenticate", "ApiKey realm=\"shop\"")
        .type(Problem.MEDIA_TYPE).entity(problem).build());
```

**Check:**

| Request | Result |
|---------|--------|
| `POST /api/products` without `X-Api-Key` | 401 + `WWW-Authenticate` + problem+json |
| `POST /api/products` with `X-Api-Key: wrong-key` | 401 |
| `POST /api/products` with `X-Api-Key: demo-123` | 201 + `Location` |
| `GET /api/products` without a key | 200 (reads are public) |

To use other keys, start the server with `mvn -q compile exec:java -Dshop.api.keys=key1,key2`.

---

### Part 4 - Gzip compression (`Compress` + `GzipWriterInterceptor`)

**Task:** create a `@NameBinding` annotation `@Compress` and a `WriterInterceptor` that gzips the body, **only** when the request's `Accept-Encoding` contains `gzip`. Put `@Compress` on the list method.

**What I did:**
1. `Compress` is a `@NameBinding` annotation. A name-bound provider only runs for resource methods that carry the same annotation.
2. `GzipWriterInterceptor` has `@Provider @Compress`, so it runs only for `GET /products`.
3. In `ProductResource` I added `@Compress` on `list(...)`. This is the only change in the resource.

**How it works:**
```java
String enc = requestHeaders.getHeaderString(HttpHeaders.ACCEPT_ENCODING);
if (enc == null || !enc.contains("gzip")) { ctx.proceed(); return; }   // client can't unzip -> plain body

ctx.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, "gzip");
ctx.getHeaders().add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
ctx.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);    // old length is wrong after compressing
GZIPOutputStream gzip = new GZIPOutputStream(ctx.getOutputStream());
ctx.setOutputStream(gzip);                              // JSON is written through the gzip stream
try { ctx.proceed(); } finally { gzip.finish(); }       // finish() writes the gzip trailer
```

- `@Context HttpHeaders` gives the headers of the *current* request. Jersey injects a proxy, so this is safe even though the interceptor is a single shared instance.
- `Vary: Accept-Encoding` tells caches that the response depends on that header.

**Check:**

| Request | Result |
|---------|--------|
| `GET /api/products` with `Accept-Encoding: gzip` | `Content-Encoding: gzip` + `Vary: Accept-Encoding` (139 bytes instead of 247) |
| `GET /api/products` without gzip | plain JSON, no `Content-Encoding` |
| `GET /api/products/1` with gzip | **not** compressed, because this method has no `@Compress` |

---

### Part 5 - Order of the filters

**Task:** show with temporary prints that `ApiKeyFilter` runs before `CorrelationFilter`, then remove the prints.

**Rule:** Jersey runs all `@PreMatching` filters first, then the normal ones. Inside each group, the lower `@Priority` number runs first.

| Order | Filter | Type | Priority |
|-------|--------|------|----------|
| 1 | `ApiKeyFilter` | pre-matching | `AUTHENTICATION` = 1000 |
| 2 | `CorsFilter` | pre-matching | `USER` = 5000 (default) |
| 3 | `CorrelationFilter` | normal (post-matching) | `USER` = 5000 |

**What I did:** I added `System.out.println(getClass().getSimpleName());` to the request side of the three filters and sent a request. The console printed:
```text
ApiKeyFilter
CorsFilter
CorrelationFilter
```
This confirms the API key is checked first. I removed the prints afterwards.

---

### Part 6 - Transcript and tests

- **`docs/task4-http.md`:** the full curl transcript with all response headers:
  - POST without a key (401)
  - POST with a wrong key (401)
  - POST with the right key (201)
  - OPTIONS preflight
  - correlation id echoed back
  - gzip on and off
  - plus the filter order and the server access log
- **`test-requests/thunder-collection_shop_rest_task4.json`:** the same requests as a Thunder Client collection. Each request has tests on the status code and the important headers, so you can see pass/fail at once.

---

### How to run and test

1. Start the server from `shop-rest`:
   ```bash
   mvn -q compile exec:java
   ```
   Wait for `shop-rest listening on http://localhost:8080/api/`.
2. In VS Code, open **Thunder Client → Collections**. If **shop_rest_task4** is not there, use **☰ → Import** and choose `test-requests/thunder-collection_shop_rest_task4.json`.
3. Send the requests one by one, or use **Run All**. Check the **Headers** and **Tests** tabs. Each request also prints one access-log line in the server terminal.
4. Request 3 (create Teapot) gives 201 only the first time and 409 after that, because the SKU already exists. Restart the server to reset the data.
5. Stop the server with `Ctrl+C`.

### Files changed

- **New:** `src/main/java/edu/itc/shop/api/filter/` with `CorrelationFilter`, `CorsFilter`, `ApiKeyFilter`, `Compress` and `GzipWriterInterceptor`
- **Changed:** `ProductResource`, which only got `@Compress` on `list(...)` and its import
- **New:** `docs/task4-http.md` and `test-requests/thunder-collection_shop_rest_task4.json`
