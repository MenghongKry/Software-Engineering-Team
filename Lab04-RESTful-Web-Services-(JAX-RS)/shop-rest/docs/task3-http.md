# Task 3 - Parameters and Validation

Server started with `mvn -q compile exec:java`. All commands run in Windows PowerShell with `curl.exe -i`.

## Test 1: Filtered and sorted list (200)

Products priced at 5 EUR or more, in stock only, sorted by price descending.

Command:
```powershell
curl.exe -i "http://localhost:8080/api/products?minPrice=5%20EUR&inStock=true&sort=price,desc"
```

Response:
```http
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40}]
```

Result: Tea is excluded (stock 0), and Kettle comes before Mug (higher price).

## Test 2: Unparsable money (400 from the ParamConverter)

Command:
```powershell
curl.exe -i "http://localhost:8080/api/products?minPrice=abc"
```

Response:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Cannot parse Money from 'abc'","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

Result: `MoneyConverterProvider` rejects the value and throws `BadRequestException`, which `ProblemMapper` turns into problem+json.

## Test 3: Sort field not in the whitelist (400)

Command:
```powershell
curl.exe -i "http://localhost:8080/api/products?sort=colour,asc"
```

Response:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Unknown sort field 'colour', use name, price or stock","instance":"/api/products","status":400,"title":"Bad Request","type":"about:blank"}
```

Result: `colour,asc` passes the `@Pattern` check, but `ProductService` only allows `name`, `price` and `stock`.

## Test 4: Invalid POST body (400 with field errors)

Body (`docs/requests/invalid-product.json`):
```json
{"name":"","sku":"kettle","price":0,"stock":-1}
```

Command:
```powershell
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" --data-binary "@docs/requests/invalid-product.json"
```

Response:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"detail":"Validation failed","errors":[{"field":"name","message":"must not be blank"},{"field":"price","message":"must be greater than or equal to 0.01"},{"field":"sku","message":"must match \"[A-Z]{2}-\\d{2}\""},{"field":"stock","message":"must be greater than or equal to 0"}],"instance":"/api/products","status":400,"title":"Invalid request","type":"about:blank"}
```

Result: Bean Validation finds four violations before the resource method runs; `ValidationMapper` lists one entry per field.

## Test 5: Unknown product (404 as problem+json)

Command:
```powershell
curl.exe -i http://localhost:8080/api/products/99
```

Response:
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{"detail":"Product 99 not found","instance":"/api/products/99","status":404,"title":"Not Found","type":"about:blank"}
```

Result: the plain 404 from Task 1 is now a problem+json body, without any change in the resource.

## Test 6: Unexpected error (500 without stack trace)

For this test only, `ProductResource.getById` temporarily started with:
```java
if (true) throw new IllegalStateException("boom");
```

Command:
```powershell
curl.exe -i http://localhost:8080/api/products/1
```

Response:
```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/problem+json
Connection: close
Content-Length: 154

{"detail":"reference 4d513b8a-5fcf-417b-931b-f0d7f5ce8626","instance":"/api/products/1","status":500,"title":"Internal Server Error","type":"about:blank"}
```

Server log:
```
(paste the SEVERE line with reference 4d513b8a-5fcf-417b-931b-f0d7f5ce8626 and the first lines of the stack trace)
```

Result: the client only receives a reference id; the stack trace is written to the server log with the same reference. The temporary line was removed afterwards.