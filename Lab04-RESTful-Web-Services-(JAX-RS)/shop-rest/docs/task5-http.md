# Task 5 - Pagination, caching and versioning

Server started fresh with `mvn -q compile exec:java` (33 products: Kettle, Mug, Tea and Item 4 .. Item 33). Writes use the default key `demo-123`.

## Test 1: Page 1 of size 10 (Link + X-Total-Count)

Command:
```bash
curl -si 'localhost:8080/api/products?page=1&size=10'
```

Response:
```http
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="first"
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="prev"
Link: <http://localhost:8080/api/products?size=10&page=2>; rel="next"
Link: <http://localhost:8080/api/products?size=10&page=3>; rel="last"
X-Total-Count: 33
X-Correlation-Id: 188dcbfc-be84-472f-94ce-619080941c85
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 659

[{"id":20,"name":"Item 20","price":20.00,"sku":"IT-20","stock":0},{"id":21,"name":"Item 21","price":21.00,"sku":"IT-21","stock":21},{"id":22,"name":"Item 22","price":22.00,"sku":"IT-22","stock":22},{"id":23,"name":"Item 23","price":23.00,"sku":"IT-23","stock":23},{"id":24,"name":"Item 24","price":24.00,"sku":"IT-24","stock":24},{"id":25,"name":"Item 25","price":25.00,"sku":"IT-25","stock":0},{"id":26,"name":"Item 26","price":26.00,"sku":"IT-26","stock":26},{"id":27,"name":"Item 27","price":27.00,"sku":"IT-27","stock":27},{"id":28,"name":"Item 28","price":28.00,"sku":"IT-28","stock":28},{"id":29,"name":"Item 29","price":29.00,"sku":"IT-29","stock":29}]
```

Result: 33 products after filtering (3 seeded + 30 from the loop). Page 1 is the second page, so all four relations are there: `first` and `prev` point to page 0, `next` to page 2, `last` to page 3 (33 / 10 rounded up = 4 pages: 0..3). The links are built with `UriInfo.getRequestUriBuilder().replaceQueryParam("page", n)`, so `size=10` is kept and nothing is hard-coded.

## Test 2: Last page has no next link

Command:
```bash
curl -si 'localhost:8080/api/products?page=3&size=10' | grep -i -E '^HTTP|^link|total'
```

Response:
```http
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/products?size=10&page=0>; rel="first"
Link: <http://localhost:8080/api/products?size=10&page=2>; rel="prev"
Link: <http://localhost:8080/api/products?size=10&page=3>; rel="last"
X-Total-Count: 33
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

Result: On the last page `next` is left out (and on page 0 `prev` is left out). Only 3 items are on this page.

## Test 3: Invalid page size (400)

Command:
```bash
curl -si 'localhost:8080/api/products?size=500'
```

Response:
```http
HTTP/1.1 400 Bad Request
X-Correlation-Id: 0f810caa-06a2-4dfc-a963-5515109673ac
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Connection: close
Content-Length: 191

{"detail":"Validation failed","errors":[{"field":"size","message":"must be less than or equal to 100"}],"instance":"/api/products","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: `size` has `@Min(1) @Max(100)`, so Bean Validation rejects it with the usual problem+json body.

## Test 4: GET with ETag and Cache-Control

Command:
```bash
curl -si localhost:8080/api/products/1
```

Response:
```http
HTTP/1.1 200 OK
ETag: "1"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: 9b1c59b0-3e64-4832-a0ad-b9243560eecf
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 63

{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12}
```

Result: The ETag is the product version (`1`, written in hex). `Cache-Control: private, no-transform, max-age=60` lets the browser keep it for one minute.

## Test 5: If-None-Match with the current tag (304)

Command:
```bash
curl -si localhost:8080/api/products/1 -H 'If-None-Match: "1"'
```

Response:
```http
HTTP/1.1 304 Not Modified
ETag: "1"
X-Correlation-Id: 994a284b-01b5-4812-84e7-295e05d90b3e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
```

Result: `request.evaluatePreconditions(tag)` sees that the client already has version 1 and returns a 304 builder. No body is sent.

## Test 6: PUT without If-Match (428)

Command:
```bash
curl -si -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Response:
```http
HTTP/1.1 428 Precondition Required
X-Correlation-Id: 325eaa9c-5fde-469a-a5ca-e26573eef872
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 140

{"detail":"If-Match header required for PUT","instance":"/api/products/1","status":428,"title":"Precondition Required","type":"about:blank"}
```

Result: PUT now needs `If-Match`. Without it the answer is 428 Precondition Required as problem+json, and the product is not changed.

## Test 7: PUT with a stale tag (412)

Command:
```bash
curl -si -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'If-Match: "0"' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Response:
```http
HTTP/1.1 412 Precondition Failed
X-Correlation-Id: 6d427405-67f5-4a32-a9f8-ef57952b0fe5
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 159

{"detail":"Product 1 is at version 1, If-Match was for version 0","instance":"/api/products/1","status":412,"title":"Precondition Failed","type":"about:blank"}
```

Result: The client edited version 0, but the product is at version 1. `ProductService.replace` compares the versions inside a `synchronized` method and throws `StaleVersionException` (412).

## Test 8: PUT with the right tag (200 + new ETag)

Command:
```bash
curl -si -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'If-Match: "1"' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
```

Response:
```http
HTTP/1.1 200 OK
ETag: "2"
X-Correlation-Id: c3577824-cfe5-4f73-ad34-b24aa2433cc2
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 66

{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

Result: The tag matches, so the product is saved. The version goes from 1 to 2 and the response carries the new `ETag: "2"` for the next edit.

## Test 9: v1 of the product (plain JSON client)

Command:
```bash
curl -si localhost:8080/api/products/1 -H 'Accept: application/json'
```

Response:
```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: 2129e551-6bb9-4e55-b709-8a7a001b7e18
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 66

{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

Result: `application/json` (and `application/vnd.shop.v1+json`) still give the v1 shape: price is a number. Plain clients do not notice v2.

## Test 10: v2 of the same product

Command:
```bash
curl -si localhost:8080/api/products/1 -H 'Accept: application/vnd.shop.v2+json'
```

Response:
```http
HTTP/1.1 200 OK
ETag: "2"
Cache-Control: private, no-transform, max-age=60
X-Correlation-Id: f98b96e6-290a-4eee-8af6-8203edda8338
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/vnd.shop.v2+json
Content-Length: 111

{"available":true,"id":1,"name":"Kettle XL","price":{"amount":29.90,"currency":"EUR"},"sku":"KT-01","stock":12}
```

Result: The vendor media type selects the second `GET {id}` method. v2 has the price as a Money object and an `available` flag. Same ETag, because it is the same version of the product.

## Test 11: No Accept header gets v1

Command:
```bash
curl -s localhost:8080/api/products/1
```

Response:
```http
{"id":1,"name":"Kettle XL","price":29.90,"sku":"KT-01","stock":12}
```

Result: curl sends `Accept: */*`. v1 is declared with `qs=1.0` and v2 with `qs=0.5`, so Jersey picks v1.

## Test 12: POST a review (201 + Location)

Command:
```bash
curl -si -X POST localhost:8080/api/products/1/reviews -H 'X-Api-Key: demo-123' \
     -H 'Content-Type: application/json' -d '{"stars":5,"author":"Ana","text":"Boils fast."}'
```

Response:
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/products/1/reviews/1
X-Correlation-Id: 42135c38-6349-4347-80b3-d9cb041e9dbe
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 68

{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}
```

Result: The locator `@Path("{id}/reviews")` in `ProductResource` loads product 1 and returns a new `ReviewResource`. Its POST validates the body and answers 201 with a Location built from `UriInfo`.

## Test 13: POST an invalid review (400)

Command:
```bash
curl -si -X POST localhost:8080/api/products/1/reviews -H 'X-Api-Key: demo-123' \
     -H 'Content-Type: application/json' -d '{"stars":9,"author":"","text":"Bad"}'
```

Response:
```http
HTTP/1.1 400 Bad Request
X-Correlation-Id: c5b4214e-155f-4d56-ab4e-6500a95dda2c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Connection: close
Content-Length: 249

{"detail":"Validation failed","errors":[{"field":"author","message":"must not be blank"},{"field":"stars","message":"must be less than or equal to 5"}],"instance":"/api/products/1/reviews","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: `@Min(1) @Max(5) stars` and `@NotBlank author` also work in the sub-resource.

## Test 14: GET the reviews

Command:
```bash
curl -si localhost:8080/api/products/1/reviews
```

Response:
```http
HTTP/1.1 200 OK
X-Correlation-Id: 14bf55fd-bdb4-4ff8-95d5-13fd19f17adf
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 70

[{"author":"Ana","id":1,"productId":1,"stars":5,"text":"Boils fast."}]
```

Result: The review from Test 12 is listed.

## Test 15: Reviews of product 999 (404)

Command:
```bash
curl -si localhost:8080/api/products/999/reviews
```

Response:
```http
HTTP/1.1 404 Not Found
X-Correlation-Id: add236db-1d86-485f-b360-c35a73fbb54b
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 127

{"detail":"Product 999 not found","instance":"/api/products/999/reviews","status":404,"title":"Not Found","type":"about:blank"}
```

Result: The locator calls `service.get(999)`, which throws `ProductNotFoundException` before `ReviewResource` is created, so the 404 comes from the locator.
