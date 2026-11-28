# Task 1 – Spring Security Test Session

## Test 1: Anonymous GET

**Command:**
```powershell
curl.exe -i http://localhost:8080/api/products
```

**Actual result:** HTTP 200 OK

**Observation:** Anonymous users can retrieve all products because GET `/api/products/**` is configured with `permitAll()`.

## Test 2: Anonymous POST

**Command:**
```powershell
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{"id":0,"name":"Mouse","price":19.9,"stock":5}'
```

**Actual result:** HTTP 401 Unauthorized

**Content-Type:** application/problem+json

**Observation:** Anonymous users cannot create products because POST requests require authentication.

## Test 3: CUSTOMER DELETE

**Command:**
```powershell
curl.exe -i -u alice:alice -X DELETE http://localhost:8080/api/products/1
```

**Actual result:** HTTP 403 Forbidden

**Content-Type:** application/problem+json

**Observation:** Alice successfully authenticated but cannot delete products because she has the CUSTOMER role rather than ADMIN.

## Test 4: ADMIN DELETE

**Command:**
```powershell
curl.exe -i -u admin:admin -X DELETE http://localhost:8080/api/products/1
```

**Actual result:** HTTP 204 No Content

**Observation:** Admin successfully deleted product ID 1 because the ADMIN role has permission to delete products.

## Summary

All four security tests passed.

| Test | Expected | Actual |
|---|---|---|
| Anonymous GET | 200 | 200 |
| Anonymous POST | 401 | 401 |
| CUSTOMER DELETE | 403 | 403 |
| ADMIN DELETE | 204 | 204 |