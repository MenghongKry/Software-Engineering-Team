## Part 5: N+1, performance tuning and a second migration

### Files Changed
- **Repository:** `OrderRepository.java` (3 new methods: `findByStatus(OrderStatus)`, `findByStatusWithLines`, `findWithGraphByStatus`)
- **Service:** `ReportService.java` (new)
- **Configuration:** `application.yml` (JDBC batching), `db/migration/V2__indexes.sql` (new)
- **Test helpers:** `support/QueryCounter.java`, `support/TestData.java` (new)
- **Tests:** `NPlusOneTest`, `DefaultBatchFetchSizeTest`, `ReportTimingTest`, `BatchInsertTest`, `OpenInViewTest`, `MigrationTest` (new)

### 1. The N+1 problem
`ReportService.linesPerPaidOrder()` loads every PAID order and then calls `o.getLines().size()` on each one. Because `lines` is LAZY, Hibernate loads the orders with one SELECT and then runs **one extra SELECT per order** to load its lines:

```
select o1_0.id,o1_0.customer_id,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 where o1_0.status=?
select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.amount,l1_0.currency from order_line l1_0 where l1_0.order_id=?
select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.amount,l1_0.currency from order_line l1_0 where l1_0.order_id=?
... (48 more)
```

`NPlusOneTest` seeds 50 PAID orders with 3 lines each (through `TestData`) and counts statements with `QueryCounter` (`Statistics.getPrepareStatementCount()`). The naive version runs **51 statements** (1 + 50). The test is tagged `@Tag("demo")` because it documents the problem.

### 2. Three fixes

| Fix | How | Statements |
|---|---|---:|
| A. join fetch | `select distinct o from Order o join fetch o.lines where o.status = :s` | 1 |
| B. `@EntityGraph` | `@EntityGraph(attributePaths = {"lines", "lines.product"}) findWithGraphByStatus` | 1 |
| C. batch fetching (16) | same naive code, lines loaded 16 orders at a time | 1 + ceil(50/16) = **5** |

Fix C's SQL uses an `IN` list instead of one query per order:
```
select l1_0.order_id,l1_0.id,... from order_line l1_0 where l1_0.order_id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
```

**Why fix C is not an annotation on the entity.** The lab says to put `@BatchSize(size = 16)` on `Order.lines`. But an annotation on the entity changes *every* query that loads lines, including the naive one, so the naive test could never show 51 statements again. Both tests cannot pass at the same time. So the batched method turns batching on only for its own transaction:

```java
em.unwrap(Session.class).setFetchBatchSize(16);   // same effect as @BatchSize(16), only in this tx
```

For the same reason `hibernate.default_batch_fetch_size: 32` is **not** in `application.yml`; with it, the naive report would only run 3 statements. `DefaultBatchFetchSizeTest` turns it on in a separate test context instead (see section 4).

### 3. Timings
`ReportTimingTest` runs each variant 5 times to warm up, then 20 times, and prints the median. H2 runs in memory, so all numbers are small and change a little between runs; the statement counts are the important column.

| variant | statements | median ms |
|---|---:|---:|
| naive (lazy loop) | 51 | 7.6 |
| join fetch | 1 | 2.0 |
| @EntityGraph | 1 | 4.3 |
| batch size 16 | 5 | 5.5 |

(Measured with the SQL log switched off: `./mvnw test -Dtest=ReportTimingTest -Dlogging.level.org.hibernate.SQL=INFO -Dlogging.level.org.hibernate.orm.jdbc.bind=INFO`.)

### 4. Adding the product name: which fixes break again?
Next I added the product name to the report (`line.getProduct().getName()`). `OrderLine.product` is also LAZY, and in the test data every line has its own product, so each product name can cost one more SELECT:

| variant | statements | median ms | what happened |
|---|---:|---:|---|
| naive + product name | **201** | 8.5 | 1 + 50 lines + 150 products |
| join fetch + product name | **151** | 4.7 | N+1 is back: the join fetch only covers `o.lines`, not `l.product` |
| @EntityGraph + product name | 1 | 2.3 | still 1, because `"lines.product"` is in the graph |
| batch size 16 + product name | 15 | 8.1 | 1 + ceil(50/16) + ceil(150/16) = 1 + 4 + 10 |

`spring.jpa.properties.hibernate.default_batch_fetch_size=32` is a global safety net: every lazy collection and lazy proxy is then loaded up to 32 at a time. `DefaultBatchFetchSizeTest` proves it caps the regression:
- naive + product name: 201 → **8** (1 + ceil(50/32) + ceil(150/32) = 1 + 2 + 5)
- join fetch + product name: 151 → **6** (1 + ceil(150/32))

The real fix for the join fetch is to also fetch the product (`join fetch o.lines l join fetch l.product`), like `findWithLinesByCustomerEmail` from Task 3 already does.

### 5. JDBC batching
`application.yml` now has:
```yaml
spring.jpa.properties.hibernate:
  jdbc.batch_size: 50
  order_inserts: true
  order_updates: true
```
`BatchInsertTest` saves 1 000 products with `saveAll` in one transaction:
```
BatchInsertTest: 1000 inserts, 22 prepared statements
TRACE org.hibernate.orm.jdbc.batch : Executing JDBC batch (50 / 50) - [edu.itc.se.shop_jpa.domain.Product#INSERT]
```
`getEntityInsertCount()` is 1 000, but only 22 statements were prepared: 20 × `next value for product_seq` (allocationSize 50 means one call per 50 ids) plus the INSERT statement, sent as 20 batches of 50. Note: in Hibernate 7 the batch logger is `org.hibernate.orm.jdbc.batch` (the lab text says `org.hibernate.engine.jdbc.batch`, which logs nothing).

**IDENTITY comparison.** I changed `Product` to `GenerationType.IDENTITY` (and its id column to `GENERATED BY DEFAULT AS IDENTITY`) in a scratch copy only. The same test then failed with **1000 prepared statements** and no "Executing JDBC batch" line. With IDENTITY the database creates the id during the INSERT, and Hibernate needs that id immediately, so it must send every INSERT on its own. That is why the project uses SEQUENCE with allocationSize 50.

### 6. open-in-view = false
`OpenInViewTest` proves the setting matters:
- `spring.jpa.open-in-view` is `false`.
- If the order is loaded by the repository outside a service transaction (what a controller would do), mapping it to `OrderDto` fails:
  `org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'edu.itc.se.shop_jpa.domain.Order.lines' ... (no session)`
- Calling `OrderService.view(id)`, which maps to the DTO **inside** its `@Transactional(readOnly = true)` method, works.

### 7. Flyway V2: indexes
`V2__indexes.sql` adds three indexes. V1 was not touched. On start-up Flyway now reports:
```
Migrating schema "PUBLIC" to version "1 - init"
Migrating schema "PUBLIC" to version "2 - indexes"
Successfully applied 2 migrations to schema "PUBLIC", now at version v2
```
`MigrationTest` checks that `flyway_schema_history` contains `1 - init` and `2 - indexes`.

**EXPLAIN before (V1 only) and after (V2):**

| query | before V2 | after V2 |
|---|---|---|
| `/api/orders` (`where status = ? order by placed_at desc`) | `PUBLIC.ORDERS.tableScan` | `PUBLIC.ORDERS.tableScan` (no change) |
| orders of a customer (`where customer_id = ? order by placed_at desc`) | `FK_ORDERS_CUSTOMER_INDEX_8: CUSTOMER_ID = ?` | `FK_ORDERS_CUSTOMER_INDEX_8: CUSTOMER_ID = ?` |
| lines of an order (`where order_id = ?`) | `FK_ORDER_LINE_ORDER_INDEX_4: ORDER_ID = ?` | `FK_ORDER_LINE_ORDER_INDEX_4: ORDER_ID = ?` |
| product search (`where category = ? and amount <= ?`) | `PUBLIC.PRODUCT.tableScan` | `IDX_PRODUCT_CATEGORY_AMOUNT: CATEGORY = 'HARDWARE' AND AMOUNT <= 60` |

Full plan for `/api/orders` after V2:
```
SELECT "O"."ID", "O"."STATUS", "O"."PLACED_AT", "C"."NAME"
FROM "PUBLIC"."ORDERS" "O"
    /* PUBLIC.ORDERS.tableScan */
    /* WHERE O.STATUS = 'PAID' */
INNER JOIN "PUBLIC"."CUSTOMER" "C"
    /* PUBLIC.PRIMARY_KEY_5: ID = O.CUSTOMER_ID */
WHERE ("O"."STATUS" = 'PAID')
ORDER BY 3 DESC
OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY
```

What I learned from the plans:
- `/api/orders` filters on `status`, and none of the three indexes starts with `status`, so it is still a table scan. An index like `orders (status, placed_at DESC)` would be needed for it.
- H2 already creates an index for every foreign key (`FK_..._INDEX_n`), so `idx_order_line_order` and `idx_orders_customer_placed` add little on H2. On PostgreSQL, which does not index foreign keys automatically, they would matter.
- `idx_product_category_amount` is the one that clearly changes a plan: the product search goes from a table scan to an index range scan.

To reproduce the "before" plan: run the app with `--spring.flyway.target=1`, open http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:shop`), and run `EXPLAIN select ...`.

### Test result
```
./mvnw verify
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
```
