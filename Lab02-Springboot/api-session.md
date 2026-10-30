# Product API curl session

Recorded against `http://localhost:8080` on 2026-10-28. The calls cover all
product endpoints and their successful and error responses. The created test
product (`api-session-20261028`, ID `86`) was deleted at the end.

## List and get

```sh
curl -i 'http://localhost:8080/api/products?q=Keyboard'
```

`200 OK`

```json
[{"id":76,"name":"Keyboard","price":25.00,"stock":10}]
```

```sh
curl -i http://localhost:8080/api/products/3
```

`200 OK`

```json
{"id":3,"name":"Monitor","price":189.99,"stock":5}
```

```sh
curl -i http://localhost:8080/api/products/999999
```

`404 Not Found` (`application/problem+json`)

```json
{"detail":"product 999999 not found","instance":"/api/products/999999","status":404,"title":"Not Found"}
```

## Create and validation/duplicate errors

```sh
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"api-session-20261028","price":1.25,"stock":0}'
```

`201 Created`, `Location: /api/products/86`

```json
{"id":86,"name":"api-session-20261028","price":1.25,"stock":0}
```

```sh
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"api-session-20261028","price":1.25,"stock":0}'
```

`409 Conflict` (`application/problem+json`)

```json
{"detail":"A record with the same unique value already exists.","instance":"/api/products","status":409,"title":"Conflict"}
```

```sh
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"","price":0,"stock":1}'
```

`400 Bad Request` (`application/problem+json`, two field errors)

```json
{"detail":"Request validation failed.","instance":"/api/products","status":400,"title":"Bad Request","errors":[{"field":"name","message":"must not be blank"},{"field":"price","message":"must be greater than or equal to 0.01"}]}
```

An out-of-range database value exercises the generic fallback:

```sh
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"api-session-overflow","price":99999999999,"stock":1}'
```

`500 Internal Server Error` (`application/problem+json`); the response contains
only the generic detail, not the database error:

```json
{"detail":"An unexpected error occurred.","instance":"/api/products","status":500,"title":"Internal Server Error"}
```

## Update stock

```sh
curl -i -X PATCH http://localhost:8080/api/products/86/stock \
  -H 'Content-Type: application/json' \
  -d '{"delta":-1}'
```

`409 Conflict` (`application/problem+json`)

```json
{"detail":"not enough stock in product 86","instance":"/api/products/86/stock","status":409,"title":"Conflict"}
```

```sh
curl -i -X PATCH http://localhost:8080/api/products/86/stock \
  -H 'Content-Type: application/json' \
  -d '{"delta":2}'
```

`200 OK`

```json
{"id":86,"name":"api-session-20261028","price":1.25,"stock":2}
```

## Delete

```sh
curl -i -X DELETE http://localhost:8080/api/products/86
```

`204 No Content`

```sh
curl -i -X DELETE http://localhost:8080/api/products/86
```

`404 Not Found` (`application/problem+json`)

```json
{"detail":"product 86 not found","instance":"/api/products/86","status":404,"title":"Not Found"}
```
