# All commands, Task 1 to the challenge

Run from `shop-rest` on macOS (zsh). Because the final code includes Task 4 and Task 5, every write needs `-H 'X-Api-Key: demo-123'` and every product PUT needs `If-Match`. Restart the server before each task so the ids and ETags match the ones below.

## Build, test, run

```bash
mvn -q verify
```

```bash
mvn -q compile exec:java
```

Stop the server with `Ctrl+C`.

## Task 1: first resource

```bash
curl -i localhost:8080/api/products
curl -i localhost:8080/api/products/2
curl -i localhost:8080/api/products/99
curl -i localhost:8080/api/products/abc
```

## Task 2: CRUD, JSON and XML

```bash
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/xml' \
     -H 'Accept: application/xml' -d '<product><name>Cup</name><sku>CU-01</sku><price>3.50</price><stock>8</stock></product>'
curl -s localhost:8080/api/products/35 -H 'Accept: application/xml'
curl -i -X PUT localhost:8080/api/products/34 -H 'X-Api-Key: demo-123' -H 'If-Match: "1"' \
     -H 'Content-Type: application/json' -d '{"name":"Teapot L","sku":"TP-01","price":35.00,"stock":5}'
curl -i -X PATCH localhost:8080/api/products/2/stock -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -d '{"delta":-5}'
curl -i -X PATCH localhost:8080/api/products/3/stock -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' -d '{"delta":-1}'
curl -i -X DELETE localhost:8080/api/products/34 -H 'X-Api-Key: demo-123'
curl -i -X DELETE localhost:8080/api/products/34 -H 'X-Api-Key: demo-123'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: text/plain' -d 'Teapot'
```

Expected: 201, 409, 201 (XML), XML body, 200, 200, 409, 204, 404, 415.

## Task 3: parameters and validation

```bash
curl -i 'localhost:8080/api/products?minPrice=5%20EUR&inStock=true&sort=price,desc'
curl -i 'localhost:8080/api/products?minPrice=abc'
curl -i 'localhost:8080/api/products?minPrice=5%20USD'
curl -i 'localhost:8080/api/products?sort=colour,asc'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"","sku":"kettle","price":0,"stock":-1}'
curl -i localhost:8080/api/products/99
```

Expected: 200, 400, 400, 400, 400 with 4 errors, 404 problem+json. The 500 experiment needs a temporary `throw new IllegalStateException()` in a resource method; see `docs/task3-http.md`.

## Task 4: filters and interceptors

```bash
curl -i -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: wrong-key' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
curl -i -X POST localhost:8080/api/products -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Teapot","sku":"TP-01","price":31.00,"stock":5}'
curl -i -X OPTIONS localhost:8080/api/products -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST'
curl -si localhost:8080/api/products/1 -H 'X-Correlation-Id: order-check-42' | grep -i correlation
curl -sv --compressed localhost:8080/api/products -o /dev/null 2>&1 | grep -i -E 'encoding|vary'
curl -sv localhost:8080/api/products -o /dev/null 2>&1 | grep -i -E 'encoding|vary'
curl -sv --compressed localhost:8080/api/products/1 -o /dev/null 2>&1 | grep -i -E 'encoding|vary'
```

Expected: 401, 401, 201, 200 with CORS headers, echoed id, gzip, no gzip, no gzip (item is not `@Compress`). Watch the server terminal for the access-log lines.

## Task 5: paging, caching, versioning

```bash
curl -si 'localhost:8080/api/products?page=1&size=10' | grep -i -E '^HTTP|link|total'
curl -si 'localhost:8080/api/products?page=3&size=10' | grep -i -E '^HTTP|link|total'
curl -i 'localhost:8080/api/products?size=500'
curl -si localhost:8080/api/products/1 | grep -i -E '^HTTP|etag|cache'
curl -si localhost:8080/api/products/1 -H 'If-None-Match: "1"'
curl -i -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
curl -i -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'If-Match: "0"' -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
curl -i -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'If-Match: "1"' -H 'Content-Type: application/json' \
     -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}'
curl -s localhost:8080/api/products/1 -H 'Accept: application/json'
curl -s localhost:8080/api/products/1 -H 'Accept: application/vnd.shop.v2+json'
curl -i -X POST localhost:8080/api/products/1/reviews -H 'X-Api-Key: demo-123' -H 'Content-Type: application/json' \
     -d '{"stars":5,"author":"Ana","text":"Boils fast."}'
curl -s localhost:8080/api/products/1/reviews
curl -i localhost:8080/api/products/999/reviews
```

Expected: 200 + 4 links + `X-Total-Count: 33`, no `next`, 400, `ETag: "1"`, 304, 428, 412, 200 + `ETag: "2"`, v1, v2, 201 + Location, list, 404.

## Challenge: orders, idempotency, OpenAPI

```bash
curl -i -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Idempotency-Key: 7c1e' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":1,"quantity":2}]}'
curl -i -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Idempotency-Key: 7c1e' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":1,"quantity":2}]}'
curl -i -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Idempotency-Key: 7c1e' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":2,"quantity":1}]}'
curl -i -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":1,"quantity":2}]}'
curl -s localhost:8080/api/products/1
curl -i localhost:8080/api/orders/o-1001
curl -i -X PUT localhost:8080/api/orders/o-1001/payment -H 'X-Api-Key: demo-123'
curl -i -X PUT localhost:8080/api/orders/o-1001/payment -H 'X-Api-Key: demo-123'
curl -i -X DELETE localhost:8080/api/orders/o-1001 -H 'X-Api-Key: demo-123'
curl -s -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Idempotency-Key: second' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":2,"quantity":5}]}'
curl -i -X DELETE localhost:8080/api/orders/o-1002 -H 'X-Api-Key: demo-123'
curl -s localhost:8080/api/products/2
curl -s localhost:8080/api/openapi | head -20
```

Expected: 201 (`Replayed: false`), 201 (`Replayed: true`), 422, 428, stock 10, 200, 200 PAID, 409, 409, 201 `o-1002`, 200 CANCELLED, Mug stock 40, the YAML document.

```bash
mvn -q verify && cat target/surefire-reports/edu.itc.shop.ShopApiTest.txt
```

Expected: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`.
