# Lab-04 demo script (Task 5 + challenge)

About 10 minutes. Show the request, the status and the headers, then say one sentence on *where* in the code it happens.

## Bring / have ready

- [ ] Laptop with JDK 25, Maven and Postman (or VS Code + Thunder Client) (`java -version`, `mvn -version`)
- [ ] The project already built once (`mvn -q verify`), so nothing has to download in class
- [ ] Two terminals in `shop-rest`: one for the server, one for curl / `mvn verify`
- [ ] Collections imported. **Postman:** Import → the three `test-requests/postman_shop_rest_*.postman_collection.json` files. **Thunder Client:** ☰ → Import →
  - `test-requests/thunder-collection_shop_rest_task5.json` (18 requests)
  - `test-requests/thunder-collection_shop_rest_challenge.json` (19 requests)
  - `test-requests/thunder-collection_shop_rest_task4.json` (in case they ask about Task 4)
- [ ] API key for writes: `X-Api-Key: demo-123` (already in the collections)
- [ ] Files open in the editor: `ProductResource`, `ReviewResource`, `OrderResource`, `IdempotencyStore`, `ProductService`, `ShopApiTest`
- [ ] `docs/task5-http.md` and `docs/challenge-http.md` as a backup if the live demo fails

**Important:** the collections expect fresh data (ETag `"1"`, order `o-1001`, stock 12). **Restart the server before each collection** (`Ctrl+C`, then start again). Run each collection top to bottom, in order.

## 0. Start (30 s)

```bash
mvn -q compile exec:java
```

Wait for `shop-rest listening on http://localhost:8080/api/`. Keep this terminal visible: every request prints one access-log line (Task 4).

## 1. Task 5 (5 min), collection `shop_rest_task5`

| # | Request | Show | Say |
|---|---------|------|-----|
| 1 | page 1, size 10 | `X-Total-Count: 33`, four `Link` headers | Links come from `uriInfo.getRequestUriBuilder().replaceQueryParam("page", n)`, nothing hard-coded |
| 2 | last page | no `rel="next"` | `Page.hasNext()` in the domain |
| 3 | `size=500` | 400 problem+json | `@Max(100)` on `ProductFilter.size` |
| 5 | GET product 1 | `ETag: "1"`, `Cache-Control: private, no-transform, max-age=60` | ETag = version |
| 6 | `If-None-Match: "1"` | 304, no body | `request.evaluatePreconditions(tag)` |
| 7 | PUT without If-Match | 428 | protects against lost updates |
| 8 | PUT with `"0"` | 412 | version check in `ProductService.replace` (synchronized) |
| 9 | PUT with `"1"` | 200, `ETag: "2"` | client gets the new tag right away |
| 10 | same old tag again | 412 | the second writer loses |
| 11–13 | v1 / v1 vendor / v2 | v2 has `price` as an object and `available` | two GET methods, `qs=1.0` vs `qs=0.5` so v1 is the default |
| 15–18 | reviews | 201 + Location, 400, list, 404 for product 999 | sub-resource locator: the 404 comes from the locator before `ReviewResource` exists |

Use **Run collection** (Postman) or **Run All** (Thunder Client) at the end to show all tests green.

## 2. Challenge (4 min), restart the server first, collection `shop_rest_challenge`

| # | Request | Show | Say |
|---|---------|------|-----|
| 1 | place order | 201, Location `o-1001`, `Link` self/pay/cancel/product-1, `Idempotency-Replayed: false` | same links in `_links` |
| 2 | same request again | 201, `Idempotency-Replayed: true`, same Location | `computeIfAbsent` in `IdempotencyStore` |
| 3 | product 1 | stock 10, not 8 | stock reserved once |
| 4 | same key, other body | 422 | SHA-256 of the body differs |
| 5 | no key | 428 | |
| 6–9 | unknown product / no stock / quantity 0 / no API key | 404 / 409 / 400 / 401 | stock check is all-or-nothing in `ProductService.reserve` |
| 10–11 | follow `self`, then `pay` | PAID, `pay` and `cancel` links gone | client follows rels only |
| 12–13 | pay again / cancel paid | 409, 409 | rules in `OrderService` |
| 14–17 | second order, cancel | Mug stock 40 → 35 → 40 | cancel gives stock back |
| 19 | `/openapi` | `application/yaml` | generated at build time by the SmallRye plugin |

## 3. Tests (1 min), second terminal

```bash
mvn -q verify && cat target/surefire-reports/edu.itc.shop.ShopApiTest.txt
```

Show `Tests run: 23, Failures: 0`. Open `ShopApiTest` and point at:
- one test per status code (`status200_…` to `status428_…`)
- `idempotency_concurrentRetriesCreateOneOrder` (10 parallel POSTs, one order)
- the test server runs on port 9998 and the links follow, which proves they come from `UriInfo`

## 4. If they ask

- **"No business rules in the resource?"** Rules are in `ProductService` / `OrderService`. Resources only map HTTP to calls and have no try/catch.
- **"Thread safety?"** Shared state is in `ConcurrentHashMap` / `AtomicLong`, writes are `synchronized`, and resources only have `final` fields.
- **"JAX-RS vs Spring MVC?"** See the section in `README.md` (same two endpoints both ways + 8 differences).
- **"Why 422 by hand?"** Jakarta REST 4.0 has no `Response.Status` constant for 422 (`HttpErrors.unprocessable`).

## Quick curl fallback (no Thunder Client)

```bash
curl -si 'localhost:8080/api/products?page=1&size=10' | grep -i -E '^HTTP|link|total'
curl -si localhost:8080/api/products/1 -H 'If-None-Match: "1"' | head -1
curl -si -X PUT localhost:8080/api/products/1 -H 'X-Api-Key: demo-123' -H 'If-Match: "1"' \
     -H 'Content-Type: application/json' -d '{"name":"Kettle XL","sku":"KT-01","price":29.90,"stock":12}' | grep -i -E '^HTTP|etag'
curl -s localhost:8080/api/products/1 -H 'Accept: application/vnd.shop.v2+json'
curl -si -X POST localhost:8080/api/orders -H 'X-Api-Key: demo-123' -H 'Idempotency-Key: 7c1e' \
     -H 'Content-Type: application/json' -d '{"lines":[{"productId":1,"quantity":2}]}' | grep -i -E '^HTTP|location|link|replayed'
```

Run the order command twice to show the replay.
