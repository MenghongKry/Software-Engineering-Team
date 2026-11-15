# Task 1 – First Resource and Server

## 1. Objective

The objective of this task is to build a simple RESTful web service using Java, Jersey (JAX-RS), and Grizzly HTTP Server. The application manages products using an in-memory repository and provides HTTP GET endpoints to retrieve product information.

## 2. Implementation

The application was implemented using the following components:

### 2.1 Product Model

Created `Product.java` as a Java record containing the following fields:

- `id`: Unique product identifier.
- `name`: Product name.
- `sku`: Stock Keeping Unit.
- `price`: Product price using `BigDecimal`.
- `stock`: Available product quantity.
- `version`: Product version number.

### 2.2 Product Repository

Created `ProductRepository.java` as an interface defining the following operations:

- `findAll()`: Retrieve all products.
- `findById(long id)`: Find a product by its ID.
- `findBySku(String sku)`: Find a product by its SKU.
- `save(Product product)`: Save or update a product.
- `delete(long id)`: Delete a product.

### 2.3 In-Memory Repository

Implemented `InMemoryProductRepository.java` using `ConcurrentHashMap` to store products in memory and `AtomicLong` to generate unique product IDs.

Three initial products were added:

| ID | Name | SKU | Price | Stock |
|---|---|---|---:|---:|
| 1 | Kettle | KT-01 | 24.90 | 12 |
| 2 | Mug | MG-01 | 6.50 | 40 |
| 3 | Tea | TE-01 | 4.20 | 0 |

The repository also maintains a version number for each product. New products start at version 1, and subsequent saves increment the version.

### 2.4 Server Configuration

Implemented `Main.java` to configure and start the Grizzly HTTP Server.

The application uses Jersey `ResourceConfig` to scan the API package and `AbstractBinder` to inject a shared `ProductRepository` instance into the resource.

The server runs at:

`http://localhost:8080/api/`

### 2.5 Product Resource

Implemented `ProductResource.java` with two GET endpoints:

| HTTP Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Retrieve all products |
| GET | `/api/products/{id}` | Retrieve a product by ID |

The resource uses constructor injection to receive the repository. It returns HTTP 404 when a product does not exist or the requested ID does not match the numeric route.

## 3. HTTP Testing

The application was started successfully using:

```bash
mvn -q compile exec:java
```

The following HTTP requests were tested using `curl.exe -i` in Windows PowerShell.

### Test 1: Retrieve All Products

**Command:**

```bash
curl.exe -i http://localhost:8080/api/products
```

**Actual Response:**

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 220

[{"id":1,"name":"Kettle","price":24.90,"sku":"KT-01","stock":12,"version":1},{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1},{"id":3,"name":"Tea","price":4.20,"sku":"TE-01","stock":0,"version":1}]
```

**Result:** Passed. The API returned all three products successfully.

### Test 2: Retrieve Product by ID

**Command:**

```bash
curl.exe -i http://localhost:8080/api/products/2
```

**Actual Response:**

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 71

{"id":2,"name":"Mug","price":6.50,"sku":"MG-01","stock":40,"version":1}
```

**Result:** Passed. The API successfully returned the product with ID 2 (Mug).

### Test 3: Retrieve a Non-Existing Product

**Command:**

```bash
curl.exe -i http://localhost:8080/api/products/99
```

**Actual Response:**

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

**Result:** Passed. The API correctly returned HTTP 404 because product ID 99 does not exist.

### Test 4: Retrieve a Product Using an Invalid ID

**Command:**

```bash
curl.exe -i http://localhost:8080/api/products/abc
```

**Actual Response:**

```http
HTTP/1.1 404 Not Found
Content-Length: 0
```

**Result:** Passed. The API correctly returned HTTP 404 because the provided ID does not match the numeric route.

## 4. Resource Design Explanation

The `ProductResource` class has no mutable fields because it only holds a final reference to the repository, which is injected through its constructor.

Although the repository is shared between requests, the resource itself does not maintain mutable instance state. This helps keep the resource stateless and avoids unnecessary shared mutable state within resource instances.

## 5. Conclusion

Task 1 was successfully completed. The RESTful web service was implemented using Java, Jersey, and Grizzly HTTP Server. The in-memory repository successfully stored and retrieved product information.

All four HTTP tests passed with the expected status codes: two requests returned HTTP 200 OK, and two requests returned HTTP 404 Not Found.