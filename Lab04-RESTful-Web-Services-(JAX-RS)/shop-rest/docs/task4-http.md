# Task 4 - Providers and filters

Server started with `mvn -q compile exec:java`. All commands run in a Linux shell (bash/zsh) with `curl`.
The API key is the default one (`demo-123`, from the system property `shop.api.keys`).

## Test 1: POST without an API key (401 problem+json)

Command:
```bash
curl -i -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Response:
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 9c8f6237-2f26-4eaa-9eda-da74666ddf25
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 125

{"detail":"Missing or invalid X-Api-Key","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

Result: `ApiKeyFilter` (`@PreMatching`) aborts before any resource is matched. The response filters still run, so the 401 also has the correlation id and the CORS headers.

## Test 2: POST with a wrong API key (401)

Command:
```bash
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: wrong-key' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Response:
```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: ApiKey realm="shop"
X-Correlation-Id: 4bd386ff-3a06-49a2-9183-38ee412ef072
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 125

{"detail":"Missing or invalid X-Api-Key","instance":"/api/products","status":401,"title":"Unauthorized","type":"about:blank"}
```

Result: the key is present but not in the allowed set, so it is rejected the same way.

## Test 3: POST with the right API key (201)

Command:
```bash
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
```

Response:
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/4
X-Correlation-Id: 97ec5988-2805-4394-a4b2-3cdb882afc0d
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 62

{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}
```

Result: the filter lets the request through and the resource creates the product as in Task 2.

## Test 4: OPTIONS preflight (200, no resource reached)

Command:
```bash
curl -i -X OPTIONS localhost:8080/api/products -H 'Origin: http://localhost:5173' \
     -H 'Access-Control-Request-Method: POST'
```

Response:
```http
HTTP/1.1 200 OK
X-Correlation-Id: 4129037c-ac9a-4c02-9b9b-477ef91adcd9
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Length: 0
```

Result: the pre-matching side of `CorsFilter` answers OPTIONS with `abortWith(Response.ok().build())`. No API key is needed (OPTIONS is a safe method) and Jersey's own OPTIONS/WADL handling is never reached.

## Test 5: GET with X-Correlation-Id echoed back

Command:
```bash
curl -i localhost:8080/api/products/1 -H 'X-Correlation-Id: order-check-42'
```

Response:
```http
HTTP/1.1 200 OK
X-Correlation-Id: order-check-42
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

Result: the id sent by the client is kept and returned. Without the header a new UUID is generated (see Tests 1-4).

## Test 6: GET /products with `--compressed` (gzip)

Command:
```bash
curl -sv --compressed localhost:8080/api/products -o /dev/null 2>&1 | grep -i -E '^> GET|^< HTTP|encoding|vary|content-length'
```

Output:
```http
> GET /api/products HTTP/1.1
> Accept-Encoding: deflate, gzip, br, zstd
< HTTP/1.1 200 OK
< Content-Encoding: gzip
< Vary: Accept-Encoding
< Content-Length: 139
```

Result: the client accepts gzip, so `GzipWriterInterceptor` compresses the list. The body is 139 bytes instead of 247 (Test 7), and `curl --compressed` decodes it back to the same JSON.

## Test 7: the same GET without Accept-Encoding (no compression)

Command:
```bash
curl -i localhost:8080/api/products
```

Response:
```http
HTTP/1.1 200 OK
X-Correlation-Id: ed1159f8-67b0-4e75-9249-8316e886f3d7
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 247

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0},{"id":4,"name":"Teapot","price":31.00,"sku":"TP-01","stock":5}]
```

Result: no `Content-Encoding` and no `Vary`, the body is plain JSON.

## Extra check: other methods are not compressed

`GET /products/{id}` has no `@Compress`, so the name-bound interceptor does not run even when the client accepts gzip:

```bash
curl -sv --compressed localhost:8080/api/products/1 -o /dev/null 2>&1 | grep -i -E '^> GET|^< HTTP|encoding|vary|content-length'
```

```http
> GET /api/products/1 HTTP/1.1
> Accept-Encoding: deflate, gzip, br, zstd
< HTTP/1.1 200 OK
< Content-Length: 63
```

## Filter order check

I temporarily added `System.out.println(getClass().getSimpleName());` to the request side of every filter, sent a GET and an authorised POST, and got this on the server console:

```text
ApiKeyFilter
CorsFilter
CorrelationFilter
```

- `ApiKeyFilter` runs first: it is `@PreMatching` with `@Priority(Priorities.AUTHENTICATION)` (1000).
- `CorsFilter` is also `@PreMatching`, but with the default priority `USER` (5000), so it runs after the key check.
- `CorrelationFilter` is a normal (post-matching) filter, so it runs only after all pre-matching filters.

The prints were removed afterwards.

## Server access log

One line per request (`METHOD path -> status [id] ms`), printed by `CorrelationFilter` with `System.Logger`:

```text
INFO: POST /api/products -> 401 [9c8f6237-2f26-4eaa-9eda-da74666ddf25] 0 ms
INFO: POST /api/products -> 401 [4bd386ff-3a06-49a2-9183-38ee412ef072] 0 ms
INFO: POST /api/products -> 201 [97ec5988-2805-4394-a4b2-3cdb882afc0d] 22 ms
INFO: OPTIONS /api/products -> 200 [4129037c-ac9a-4c02-9b9b-477ef91adcd9] 0 ms
INFO: GET /api/products/1 -> 200 [order-check-42] 0 ms
INFO: GET /api/products -> 200 [95582a1f-72b8-4d70-815e-5e3de807b728] 3 ms
INFO: GET /api/products -> 200 [ed1159f8-67b0-4e75-9249-8316e886f3d7] 0 ms
```

Note: aborted requests (401, OPTIONS) show `0 ms`. They are stopped by a pre-matching filter before the request side of `CorrelationFilter` runs, so there is no start time; the response side still generates the id and logs the line.
