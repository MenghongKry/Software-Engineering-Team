# Lab-03 Task 3 – API session

Application started with the dev profile (DataSeeder: 3 customers, 12 products, 30 orders):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

All calls made with `curl.exe` from a second PowerShell window. SQL copied from the application log (`org.hibernate.SQL=DEBUG`).

---

## 1. Orders by status – interface projection, paged and sorted

```
> curl.exe -s "http://localhost:8080/api/orders?status=PAID&page=0&size=5&sort=placedAt,desc"
{"content":[{"customer":{"name":"Grace Hopper"},"id":2,"placedAt":"2026-11-06T22:14:16.762780Z","status":"PAID"},{"customer":{"name":"Alan Turing"},"id":6,"placedAt":"2026-11-05T18:14:16.762780Z","status":"PAID"},{"customer":{"name":"Ada Lovelace"},"id":10,"placedAt":"2026-11-04T14:14:16.762780Z","status":"PAID"},{"customer":{"name":"Grace Hopper"},"id":14,"placedAt":"2026-11-03T10:14:16.762780Z","status":"PAID"},{"customer":{"name":"Alan Turing"},"id":18,"placedAt":"2026-11-02T06:14:16.762780Z","status":"PAID"}],"page":{"size":5,"number":0,"totalElements":8,"totalPages":2}}
```

SQL – two statements, only the projected columns:

```
DEBUG org.hibernate.SQL : select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.name from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id where o1_0.status=? order by 3 desc fetch first ? rows only
TRACE org.hibernate.orm.jdbc.bind : binding parameter (1:ENUM) <- [PAID]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (2:INTEGER) <- [5]
DEBUG org.hibernate.SQL : select count(o1_0.id) from orders o1_0 where o1_0.status=?
TRACE org.hibernate.orm.jdbc.bind : binding parameter (1:ENUM) <- [PAID]
```

---

## 2. Product search – Specifications mapped to the `ProductView` record

```
> curl.exe -s "http://localhost:8080/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true"
{"content":[{"id":1,"sku":"KB-1","name":"Keyboard basic","amount":39.00,"stock":10},{"id":2,"sku":"KB-7","name":"Mechanical keyboard","amount":59.00,"stock":4}],"page":{"size":20,"number":0,"totalElements":2,"totalPages":1}}
```

SQL – each of the four filters became one `AND` condition:

```
DEBUG org.hibernate.SQL : select p1_0.id,p1_0.category,p1_0.name,p1_0.amount,p1_0.currency,p1_0.sku,p1_0.stock from product p1_0 where lower(p1_0.name) like ? escape '' and p1_0.category=? and p1_0.amount<=? and p1_0.stock>? order by p1_0.sku offset ? rows fetch first ? rows only
TRACE org.hibernate.orm.jdbc.bind : binding parameter (1:VARCHAR) <- [%key%]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (2:ENUM) <- [HARDWARE]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (3:NUMERIC) <- [60]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (4:INTEGER) <- [0]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (5:INTEGER) <- [0]
TRACE org.hibernate.orm.jdbc.bind : binding parameter (6:INTEGER) <- [20]
```

---

## 3. Orders of a customer – fetch join in a read-only transaction

```
> curl.exe -s "http://localhost:8080/api/customers/ada@example.com/orders"
[{"id":1,"status":"NEW","placedAt":"2026-11-07T05:14:16.762780Z","total":268.97,"lines":[{"sku":"KB-1","productName":"Keyboard basic","quantity":1,"unitPrice":39.00},{"sku":"KB-7","productName":"Mechanical keyboard","quantity":1,"unitPrice":59.00},{"sku":"BK-3","productName":"Refactoring","quantity":3,"unitPrice":42.00},{"sku":"TY-1","productName":"Puzzle 1000 pieces","quantity":3,"unitPrice":14.99}]},{"id":4,"status":"CANCELLED","placedAt":"2026-11-06T08:14:16.762780Z","total":132.00,"lines":[{"sku":"BK-3","productName":"Refactoring","quantity":1,"unitPrice":42.00},{"sku":"BK-2","productName":"Effective Java","quantity":2,"unitPrice":45.00}]},{"id":7,"status":"SHIPPED","placedAt":"2026-11-05T11:14:16.762780Z","total":170.90,"lines":[{"sku":"MS-2","productName":"Wireless mouse","quantity":1,"unitPrice":19.90},{"sku":"FD-1","productName":"Coffee beans","quantity":2,"unitPrice":12.50},{"sku":"BK-3","productName":"Refactoring","quantity":3,"unitPrice":42.00}]},{"id":10,"status":"PAID","placedAt":"2026-11-04T14:14:16.762780Z","total":483.80,"lines":[{"sku":"BK-2","productName":"Effective Java","quantity":2,"unitPrice":45.00},{"sku":"TY-2","productName":"Robot kit","quantity":3,"unitPrice":79.00},{"sku":"MS-2","productName":"Wireless mouse","quantity":2,"unitPrice":19.90},{"sku":"KB-1","productName":"Keyboard basic","quantity":3,"unitPrice":39.00}]},{"id":13,"status":"NEW","placedAt":"2026-11-03T17:14:16.762780Z","total":141.00,"lines":[{"sku":"FD-1","productName":"Coffee beans","quantity":3,"unitPrice":12.50},{"sku":"BK-1","productName":"Clean Code","quantity":3,"unitPrice":34.50}]},{"id":16,"status":"CANCELLED","placedAt":"2026-11-02T20:14:16.762780Z","total":208.90,"lines":[{"sku":"MS-2","productName":"Wireless mouse","quantity":1,"unitPrice":19.90},{"sku":"MN-3","productName":"27\" monitor","quantity":1,"unitPrice":189.00}]},{"id":19,"status":"SHIPPED","placedAt":"2026-11-01T23:14:16.762780Z","total":72.99,"lines":[{"sku":"TY-3","productName":"Board game","quantity":2,"unitPrice":29.00},{"sku":"TY-1","productName":"Puzzle 1000 pieces","quantity":1,"unitPrice":14.99}]},{"id":22,"status":"PAID","placedAt":"2026-11-01T02:14:16.762780Z","total":606.20,"lines":[{"sku":"MN-3","productName":"27\" monitor","quantity":1,"unitPrice":189.00},{"sku":"FD-2","productName":"Dark chocolate","quantity":1,"unitPrice":3.20},{"sku":"TY-2","productName":"Robot kit","quantity":3,"unitPrice":79.00},{"sku":"KB-7","productName":"Mechanical keyboard","quantity":3,"unitPrice":59.00}]},{"id":25,"status":"NEW","placedAt":"2026-10-31T05:14:16.762780Z","total":162.97,"lines":[{"sku":"TY-1","productName":"Puzzle 1000 pieces","quantity":3,"unitPrice":14.99},{"sku":"KB-7","productName":"Mechanical keyboard","quantity":2,"unitPrice":59.00}]},{"id":28,"status":"CANCELLED","placedAt":"2026-10-30T08:14:16.762780Z","total":256.70,"lines":[{"sku":"TY-2","productName":"Robot kit","quantity":2,"unitPrice":79.00},{"sku":"FD-1","productName":"Coffee beans","quantity":3,"unitPrice":12.50},{"sku":"TY-3","productName":"Board game","quantity":2,"unitPrice":29.00},{"sku":"FD-2","productName":"Dark chocolate","quantity":1,"unitPrice":3.20}]}]
```

SQL – one statement loads orders, lines and products together (no extra selects while mapping to DTOs):

```
DEBUG org.hibernate.SQL : select distinct o1_0.id,o1_0.customer_id,l1_0.order_id,l1_0.id,l1_0.product_id,p1_0.id,p1_0.category,p1_0.name,p1_0.amount,p1_0.currency,p1_0.sku,p1_0.stock,l1_0.quantity,l1_0.amount,l1_0.currency,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 join order_line l1_0 on o1_0.id=l1_0.order_id join product p1_0 on p1_0.id=l1_0.product_id join customer c1_0 on c1_0.id=o1_0.customer_id where c1_0.email=? order by o1_0.placed_at desc
TRACE org.hibernate.orm.jdbc.bind : binding parameter (1:VARCHAR) <- [ada@example.com]
```
