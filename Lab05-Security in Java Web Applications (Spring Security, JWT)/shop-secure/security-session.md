# security-session.md — Lab-05 transcripts

Paste the real output of every command (status line, relevant headers, body). Never paste a real secret or a full JWT signing key.

## Task 1 — Default security and first filter chain

### 1a. Zero configuration (before SecurityConfig)

```text
$ curl -i localhost:8080/api/products
TODO

$ curl -i -u user:<generated password> localhost:8080/api/products
TODO
```

### 1b. With SecurityConfig

```text
$ curl -i localhost:8080/api/products                       # 200 anonymous
TODO

$ curl -i -X POST localhost:8080/api/products -H 'Content-Type: application/json' -d '{"name":"Mouse","price":19.9,"stock":5}'   # 401
TODO

$ curl -i -u alice:alice -X DELETE localhost:8080/api/products/1   # 403
TODO

$ curl -i -u admin:admin -X DELETE localhost:8080/api/products/1   # 204
TODO
```

## Task 2 — Users and passwords in the database

```text
$ curl -i -X POST localhost:8080/api/register -H 'Content-Type: application/json' -d '{"username":"bob","password":"short"}'   # 400
TODO

$ curl -i -X POST localhost:8080/api/register -H 'Content-Type: application/json' -d '{"username":"bob","password":"Correct-Horse-9"}'   # 201
TODO

$ curl -i -X POST localhost:8080/api/register ... (same username again)   # 409
TODO

$ curl -i -u bob:Correct-Horse-9 localhost:8080/api/me   # 200
TODO

-- users table (H2)
TODO   ({bcrypt}$2a$10$... row)
```

## Task 3 — Roles and method security

```text
$ curl -i -u alice:... -X POST localhost:8080/api/orders -H 'Content-Type: application/json' -d '{"productId":2,"quantity":1}'   # 201
TODO

$ curl -i -u bob:... localhost:8080/api/orders/1     # 403
TODO

$ curl -i -u admin:... localhost:8080/api/orders/1   # 200
TODO

$ ./mvnw -q test -Dtest='Order*SecurityTest'
TODO
```

## Task 4 — JWT login and stateless API

```text
$ curl -s -X POST localhost:8080/api/auth/login ...           # token
TODO

$ curl -i localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"   # 200
TODO

$ curl -i -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" ...   # 201, no Set-Cookie
TODO

$ curl -i localhost:8080/api/orders -H "Authorization: Bearer $EXPIRED"   # 401 invalid_token
TODO

$ curl -i -X POST localhost:8080/api/auth/login ... wrong password   # 401
TODO
```

### Decoded JWT

```json
// header
TODO
// payload
TODO
```

Why the payload is readable and why that is fine: _TODO (one sentence)._

## Task 5 — Hardening

### CORS

```text
$ curl -i -X OPTIONS localhost:8080/api/orders -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: authorization,content-type'
TODO

$ curl -i -X OPTIONS localhost:8080/api/orders -H 'Origin: http://evil.example' -H 'Access-Control-Request-Method: POST'
TODO   (no Access-Control-Allow-Origin)
```

### Security headers

```text
$ curl -I localhost:8080/api/products
TODO
```

### Stored XSS

```text
$ curl -s localhost:8080/api/products/2/reviews
TODO
-- page source with th:text:
TODO
```

Screenshot th:text vs th:utext (unmerged branch): _TODO_

### Secrets

```text
$ git grep -n -i -E "password|secret" -- '*.yml' '*.properties' '*.java'
TODO

$ JWT_SECRET= ./mvnw -q spring-boot:run
TODO   (APPLICATION FAILED TO START)
```

### Security log (failed login, successful login, access denied)

```text
TODO
```
