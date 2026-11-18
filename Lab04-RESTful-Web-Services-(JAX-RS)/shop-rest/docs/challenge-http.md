# Challenge - Orders, HATEOAS, idempotency, OpenAPI and tests

Server started fresh with `mvn -q compile exec:java`. Writes use the default key `demo-123`. Product 1 (Kettle) starts with stock 12, product 2 (Mug) with 40, product 3 (Tea) with 0.

## Test 1: Place an order (201, links, Idempotency-Replayed: false)

Command:
```bash
curl -si -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: 7c1e' \
     -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Response:
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1001
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; rel="cancel"
Link: <http://localhost:8080/api/products/1>; rel="product-1"
Idempotency-Replayed: false
X-Correlation-Id: 87ca52e2-6ef4-45e2-bba1-1ca63c5d29dd
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"id":"o-1001","status":"NEW","total":49.80,"lines":[{"productId":1,"quantity":2}],"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"}}
```

Result: Stock of product 1 is reserved (12 -> 10). The order is NEW, so the links are `self`, `pay`, `cancel` and one `product-1` link for the line. They are both in the `Link` headers and in `_links`, all built from `UriInfo`.

## Test 2: Same request again (replay)

Command:
```bash
curl -si -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: 7c1e' \
     -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Response:
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/orders/o-1001
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; rel="cancel"
Link: <http://localhost:8080/api/products/1>; rel="product-1"
Idempotency-Replayed: true
X-Correlation-Id: d8174510-7e0c-4d0c-8598-3cd544a66a7c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"id":"o-1001","status":"NEW","total":49.80,"lines":[{"productId":1,"quantity":2}],"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"}}
```

Result: Same key and same body hash, so the stored 201 is sent again with `Idempotency-Replayed: true`. Same Location, and no second order is created.

## Test 3: Stock was reserved only once

Command:
```bash
curl -s localhost:8080/api/products/1
```

Response:
```http
{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":10}
```

Result: Stock is 10, not 8: the retry did not reserve again.

## Test 4: Same key, different body (422)

Command:
```bash
curl -si -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: 7c1e' \
     -d '{"lines":[{"productId":2,"quantity":1}]}'
```

Response:
```http
HTTP/1.1 422 Unprocessable Content
X-Correlation-Id: fd9580c0-78e7-4cdc-9ba5-d361f626528b
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 163

{"detail":"Idempotency-Key 7c1e was already used with a different body","instance":"/api/orders","status":422,"title":"Unprocessable Content","type":"about:blank"}
```

Result: The SHA-256 of the new body is different from the stored one, so the key cannot be reused: 422 problem+json.

## Test 5: No Idempotency-Key (428)

Command:
```bash
curl -si -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -d '{"lines":[{"productId":1,"quantity":2}]}'
```

Response:
```http
HTTP/1.1 428 Precondition Required
X-Correlation-Id: b80c91f2-2f18-44c2-bd91-3b272cb44662
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 135

{"detail":"Idempotency-Key header required","instance":"/api/orders","status":428,"title":"Precondition Required","type":"about:blank"}
```

Result: POST /orders requires the header.

## Test 6: Unknown product (404) and not enough stock (409)

Command:
```bash
curl -s -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: k-404' \
     -d '{"lines":[{"productId":999,"quantity":1}]}'; echo
curl -s -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: k-409' \
     -d '{"lines":[{"productId":3,"quantity":1}]}'
```

Response:
```http
{"detail":"Product 999 not found","instance":"/api/orders","status":404,"title":"Not Found","type":"about:blank"}
{"detail":"Insufficient stock: have 0, change -1","instance":"/api/orders","status":409,"title":"Conflict","type":"about:blank"}
```

Result: `ProductService.reserve` checks every line first and only then takes the stock, so a failed order changes nothing. Failures are not stored under the key.

## Test 7: Follow rel self (GET the order)

Command:
```bash
curl -si localhost:8080/api/orders/o-1001
```

Response:
```http
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/orders/o-1001/payment>; rel="pay"
Link: <http://localhost:8080/api/orders/o-1001>; rel="cancel"
Link: <http://localhost:8080/api/products/1>; rel="product-1"
X-Correlation-Id: d22286de-8016-4ed7-b8b1-322747c3864c
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 301

{"id":"o-1001","status":"NEW","total":49.80,"lines":[{"productId":1,"quantity":2}],"_links":{"self":"http://localhost:8080/api/orders/o-1001","pay":"http://localhost:8080/api/orders/o-1001/payment","cancel":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"}}
```

Result: The client uses the `self` link from Test 1.

## Test 8: Follow rel pay (NEW -> PAID)

Command:
```bash
curl -si -X PUT localhost:8080/api/orders/o-1001/payment -H 'X-Api-Key: demo-123'
```

Response:
```http
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1001>; rel="self"
Link: <http://localhost:8080/api/products/1>; rel="product-1"
X-Correlation-Id: 7266a29f-0287-4dec-97bf-760cc80efcdb
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 195

{"id":"o-1001","status":"PAID","total":49.80,"lines":[{"productId":1,"quantity":2}],"_links":{"self":"http://localhost:8080/api/orders/o-1001","product-1":"http://localhost:8080/api/products/1"}}
```

Result: Now PAID: `pay` and `cancel` are gone from the links, so the client knows these steps are not possible any more.

## Test 9: Pay again (409)

Command:
```bash
curl -si -X PUT localhost:8080/api/orders/o-1001/payment -H 'X-Api-Key: demo-123'
```

Response:
```http
HTTP/1.1 409 Conflict
X-Correlation-Id: a79abce8-6f4d-4cfb-ae01-179a3e425672
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 145

{"detail":"Order o-1001 is PAID and cannot be paid","instance":"/api/orders/o-1001/payment","status":409,"title":"Conflict","type":"about:blank"}
```

Result: Only a NEW order can be paid.

## Test 10: Cancel a paid order (409)

Command:
```bash
curl -si -X DELETE localhost:8080/api/orders/o-1001 -H 'X-Api-Key: demo-123'
```

Response:
```http
HTTP/1.1 409 Conflict
X-Correlation-Id: 1c51abea-1fd6-4c16-a70d-9f76d6151fef
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 142

{"detail":"Order o-1001 is PAID and cannot be cancelled","instance":"/api/orders/o-1001","status":409,"title":"Conflict","type":"about:blank"}
```

Result: A PAID order cannot be cancelled.

## Test 11: Place a second order, then follow rel cancel (stock restored)

Command:
```bash
curl -s -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -H 'Idempotency-Key: second' \
     -d '{"lines":[{"productId":2,"quantity":5}]}'; echo
curl -s localhost:8080/api/products/2; echo
curl -si -X DELETE localhost:8080/api/orders/o-1002 -H 'X-Api-Key: demo-123'; echo
curl -s localhost:8080/api/products/2
```

Response:
```http
{"id":"o-1002","status":"NEW","total":32.50,"lines":[{"productId":2,"quantity":5}],"_links":{"self":"http://localhost:8080/api/orders/o-1002","pay":"http://localhost:8080/api/orders/o-1002/payment","cancel":"http://localhost:8080/api/orders/o-1002","product-2":"http://localhost:8080/api/products/2"}}
{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":35}
HTTP/1.1 200 OK
Link: <http://localhost:8080/api/orders/o-1002>; rel="self"
Link: <http://localhost:8080/api/products/2>; rel="product-2"
X-Correlation-Id: 80dcd881-d961-493b-9ed4-8ae82cdba037
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/json
Content-Length: 200

{"id":"o-1002","status":"CANCELLED","total":32.50,"lines":[{"productId":2,"quantity":5}],"_links":{"self":"http://localhost:8080/api/orders/o-1002","product-2":"http://localhost:8080/api/products/2"}}
{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40}
```

Result: Mug stock goes 40 -> 35 when the order is placed and back to 40 when it is cancelled. A CANCELLED order only has `self` and product links.

## Test 12: Unknown order (404)

Command:
```bash
curl -si localhost:8080/api/orders/o-9999
```

Response:
```http
HTTP/1.1 404 Not Found
X-Correlation-Id: 5f7d9dd5-60db-4581-a25c-bcc945dcd00e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/problem+json
Content-Length: 121

{"detail":"Order o-9999 not found","instance":"/api/orders/o-9999","status":404,"title":"Not Found","type":"about:blank"}
```

Result: problem+json like every other error.

## Test 13: OpenAPI document

Command:
```bash
curl -si localhost:8080/api/openapi | head -20
```

Response:
```http
HTTP/1.1 200 OK
X-Correlation-Id: 32785afe-a07a-45d6-a8f2-0a7c479e284e
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: Content-Type,X-Api-Key,X-Correlation-Id,If-Match
Access-Control-Expose-Headers: Location,ETag,Link,X-Total-Count,X-Correlation-Id
Content-Type: application/yaml
Transfer-Encoding: chunked

---
openapi: 3.1.0
tags:
- name: meta
  description: API description
- name: orders
  description: "Placing, paying and cancelling orders (HATEOAS)"
- name: products
  description: Product catalogue
- name: reviews
  description: Reviews of one product
```

Result: Generated at build time by `smallrye-open-api-maven-plugin` (phase `process-classes`) into `target/classes/META-INF/openapi.yaml` and served by `OpenApiResource` with `Content-Type: application/yaml`.

## Test 14: Test suite

Command:
```bash
mvn -q verify > /dev/null 2>&1 && cat target/surefire-reports/edu.itc.shop.ShopApiTest.txt
```

Response:
```http
-------------------------------------------------------------------------------
Test set: edu.itc.shop.ShopApiTest
-------------------------------------------------------------------------------
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.326 s -- in edu.itc.shop.ShopApiTest
```

Result: `ShopApiTest` (JerseyTest on `Main.config()`, fresh data per test) has one test per status code 200, 201, 204, 304, 400, 401, 404, 406, 409, 412, 415, 422, 428, plus the HATEOAS flows, concurrent retries and the OpenAPI endpoint: 23 tests, all green.
