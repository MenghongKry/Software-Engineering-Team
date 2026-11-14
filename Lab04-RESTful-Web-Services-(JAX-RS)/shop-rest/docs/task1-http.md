
# Task 1 - HTTP Testing Transcript

## Test 1: Get All Products

Command:
```powershell
curl.exe -i http://localhost:8080/api/products
```

Response:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 220

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12,"version":1},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0,"version":1}]
```

## Test 2: Get Product by ID (2)

Command:
```powershell
curl.exe -i http://localhost:8080/api/products/2
```

Response:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 71

{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1}
```

## Test 3: Get Non-existing Product (99)

Command:
```powershell
curl.exe -i http://localhost:8080/api/products/99
```

Response:
```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

## Test 4: Invalid Product ID (abc)

Command:
```powershell
curl.exe -i http://localhost:8080/api/products/abc
```

Response:
```http
HTTP/1.1 404 Not Found
Content-Length: 0
```
