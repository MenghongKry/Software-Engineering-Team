## Challenge: Reporting and Bulk Module

The back office needed four things:
1. a product report with free combinations of filters
2. an order feed that scrolls forever without page numbers
3. a stock reservation that never oversells under load
4. a fast import of a 50 000-product supplier catalogue

All four are implemented and tested. `./mvnw verify` → **53 tests, 0 failures**.

### Files Changed
- **Build:** `pom.xml` (adds `hibernate-processor`, which generates the static metamodel `Product_`, `Money_`, `Tag_`, … at compile time)
- **Migration:** `db/migration/V3__product_version.sql` (new). It adds `product.version` for optimistic locking.
- **Domain:** `Product.java` (adds `@Version long version`)
- **Repository:**
  - `ProductReportRepository`, `ProductReportRepositoryImpl`, `ProductReportFilter` (new)
  - `ProductRepository`: `findForUpdate`, `decrementStock`, and it now extends the report fragment
  - `OrderRepository`: `feedSlice`, `findAllByOrderByPlacedAtDescIdDesc`
- **Service:** `ProductReportService`, `OrderFeedService`, `StockService`, `StockFacade`, `BulkImportService`, `OutOfStockException` (new)
- **Web:** `ReportController`, `ApiExceptionHandler` (new). `OrderController` gets `/orders/feed` and `/orders/feed/slice`.
- **DTOs:** `ProductReportRow`, `OrderFeed`, `OrderFeedItem` (new)
- **Tests:** `ProductReportTest` (5), `OrderFeedTest` (4), `StockReservationTest` (3), `BulkImportTest` (3) (new). `MigrationTest` now expects 3 versions.

> **IDE note:** `Product_`, `Money_` and `Tag_` are generated into `target/generated-sources/annotations` by `./mvnw compile`. Until you build once (and reload the Maven project in your IDE), the editor shows "cannot be resolved" in `ProductReportRepositoryImpl`. The code itself is fine.

---

### 1. Criteria API report: `GET /api/reports/products`

```
GET /api/reports/products?q=&category=&maxPrice=&minStock=&tag=&sort=amount,desc
```

**How it works.** `ProductRepository` extends a custom fragment `ProductReportRepository`. Spring Data finds the implementation because its name ends in `Impl`. `ProductReportRepositoryImpl` builds the query with `CriteriaBuilder`:

| Requirement | How I did it |
|---|---|
| Any subset of 5 filters | Each filter adds a `Predicate` to a list **only when it is not null**: `q` (name contains, case-insensitive), `category`, `maxPrice`, `minStock`, `tag` |
| Join to tags only when needed | `p.join(Product_.tags)` is called only inside `if (tag != null)` |
| Tag count | `cb.size(p.get(Product_.tags))` becomes a correlated sub-select, so it counts **all** tags of the product, even when the result is filtered by one tag |
| Record projection + groupBy | `cb.construct(ProductReportRow.class, sku, name, amount, stock, tagCount)` and `groupBy(id, sku, name, amount, stock)` |
| Sort whitelist | A `Map` of the allowed sort keys: `sku`, `name`, `amount`, `stock`, `tagCount`. Anything else → `IllegalArgumentException` → **400 Bad Request** |
| Metamodel, not strings | `Product_.name`, `Money_.amount`, `Tag_.name`. A typo is a compile error instead of a runtime error |

**Result:**
```
$ curl -s "localhost:8080/api/reports/products?category=HARDWARE&minStock=1&tag=sale&sort=amount,desc"
[{"sku":"KB-7","name":"Mechanical keyboard","amount":59.00,"stock":4,"tagCount":2},
 {"sku":"KB-1","name":"Keyboard basic","amount":39.00,"stock":10,"tagCount":1}]

$ curl -s "localhost:8080/api/reports/products?sort=version,desc"
{"detail":"Cannot sort by 'version'. Allowed: [stock, name, amount, tagCount, sku]","status":400,"title":"Bad Request"}
```

**SQL with all filters** (copied from the log):
```sql
select p1_0.sku c0,p1_0.name c1,p1_0.amount c2,p1_0.stock c3,
       (select count(*) from product_tag t2_0 where p1_0.id=t2_0.product_id) c4
  from product p1_0
  join product_tag t1_0 on p1_0.id=t1_0.product_id
  join tag t1_1 on t1_1.id=t1_0.tag_id
 where lower(p1_0.name) like ? escape '' and p1_0.category=? and p1_0.amount<=? and p1_0.stock>=? and t1_1.name=?
 group by p1_0.id,c0,c1,c2,c3
 order by 3 desc
```
**SQL with no filter.** There is no join at all:
```sql
select p1_0.sku c0,p1_0.name c1,p1_0.amount c2,p1_0.stock c3,
       (select count(*) from product_tag t1_0 where p1_0.id=t1_0.product_id) c4
  from product p1_0
 group by p1_0.id,c0,c1,c2,c3
 order by 1
```

**What went wrong:** my test expected `IllegalArgumentException` for an unknown sort column, but got `InvalidDataAccessApiUsageException`. Spring's repository proxy translates every exception thrown inside a repository into its own `DataAccessException` family. I changed the test to check the root cause, and `ApiExceptionHandler` handles both exception types, so the client still gets a 400.

**When to use the Criteria API:** when the *shape* of the query changes at runtime (optional joins, dynamic sort, aggregates). For fixed queries JPQL is easier to read, and for optional filters on a single entity a `Specification` (Task 3) is enough.

---

### 2. Infinite scroll: `GET /api/orders/feed`

```
GET /api/orders/feed?size=20&after=<placedAt>,<id>     → keyset
GET /api/orders/feed/slice?size=20&page=0              → Slice (offset)
```
Both return `{ items, nextCursor, hasNext }`.

**Slice version.** `Slice<OrderSummary> feedSlice(Pageable)` with a JPQL projection. Spring asks for `size + 1` rows to find out if there is a next slice, and never runs a `count` query (that is the difference from `Page`):
```sql
select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.name
  from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id
 order by o1_0.placed_at desc,o1_0.id desc
 offset ? rows fetch first ? rows only
```

**Keyset version.** `Window<Order> findAllByOrderByPlacedAtDescIdDesc(ScrollPosition, Limit)` with `@EntityGraph("customer")`. The cursor is the `placedAt,id` of the last item. The service turns it into `ScrollPosition.forward(Map.of("placedAt", …, "id", …))`:
```sql
select o1_0.id,o1_0.customer_id,c1_0.id,...,c1_0.name,...,o1_0.placed_at,o1_0.status,o1_0.version
  from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id
 where o1_0.placed_at<? or o1_0.placed_at=? and o1_0.id<?
 order by o1_0.placed_at desc,o1_0.id desc
 fetch first ? rows only
```

**Result** (dev profile, 30 orders):
```
$ curl -s "localhost:8080/api/orders/feed?size=20" | jq '{n: (.items|length), next: .nextCursor, hasNext}'
{ "n": 20, "next": "2026-11-03T23:37:23.091313Z,20", "hasNext": true }

$ curl -s "localhost:8080/api/orders/feed?size=20&after=2026-11-03T23:37:23.091313Z,20" | jq '{n: (.items|length), next: .nextCursor, hasNext}'
{ "n": 10, "next": null, "hasNext": false }
```

**Why the keyset version stays fast at offset 100 000:**
- With `OFFSET 100000` the database has to **read and skip 100 000 rows** before it returns 20, and every page is slower than the one before.
- The keyset `WHERE placed_at < ? OR (placed_at = ? AND id < ?)` lets an index on `(placed_at, id)` **jump directly** to the first row after the cursor, so page 5 000 costs the same as page 1.
- Keyset is also stable while new orders arrive. With OFFSET, a new order pushes everything down by one, so you see an item twice. With keyset you continue exactly after the last item you saw.

**Why `id` is in the sort:** two orders can have the same `placedAt`. Without a unique tie-breaker the order between them is random, and keyset scrolling could skip or repeat one. `OrderFeedTest` creates pairs of orders with the same `placedAt` on purpose and checks that all 45 orders come back exactly once, in the same order, from both implementations.

**Test highlights (`OrderFeedTest`):**
- the keyset feed walks through 45 orders in exactly 3 pages (20 + 20 + 5)
- the cursor has the form `placedAt,id`
- one keyset page = **1 SQL statement**, including the customer names (entity graph, no N+1)
- the Slice feed returns the same order

**When to use which:**
- `Page`: the user needs "page 7 of 12" (costs an extra count query).
- `Slice`: "load more" with shallow scrolling.
- Keyset `Window`: infinite feeds, deep scrolling, exports.

---

### 3. Stock reservation that never oversells

`StockReservationTest`: product with **stock = 5**, **10 threads** start at the same moment (a `CountDownLatch`), and each reserves 1 unit.

| Variant | How | Result | Time |
|---|---|---|---:|
| pessimistic | `StockService.reserve` → `@Lock(PESSIMISTIC_WRITE) findForUpdate` → `select ... for update` | 5 ok / 5 out-of-stock, stock = 0 | 24 ms |
| optimistic | `@Version` on Product + `StockFacade.reserveOptimisticWithRetry` (up to 20 attempts, small back-off) | 5 ok / 5 out-of-stock, 5 retries | 31 ms |
| atomic update | `@Modifying update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty` | 5 ok / 5 out-of-stock | 22 ms |

```
INFO  StockReservationTest : pessimistic: 5 ok / 5 out-of-stock, stock=0, 10 threads in 24 ms
INFO  StockReservationTest : optimistic : 5 ok / 5 out-of-stock after 5 retries, 31 ms
INFO  StockReservationTest : atomic upd : 5 ok / 5 out-of-stock, 22 ms
```

The SQL behind each:
```sql
-- pessimistic: the 2nd..10th thread WAIT here until the 1st commits
select p1_0.id,...,p1_0.stock,p1_0.version from product p1_0 where p1_0.id=? for update
-- optimistic: the loser updates 0 rows → ObjectOptimisticLockingFailureException → retry
update product set category=?,name=?,amount=?,currency=?,sku=?,stock=?,version=? where id=? and version=?
-- atomic: the database checks and decrements in ONE statement; 0 rows changed = out of stock
update product p1_0 set stock=(p1_0.stock-cast(? as integer)) where p1_0.id=? and p1_0.stock>=?
```

**How each one prevents overselling:**
- **Pessimistic:** the row is locked, so only one thread at a time can read-check-write. The others queue up.
- **Optimistic:** everyone reads at the same time, but the `version` check lets only one UPDATE through per version. The losers reload the fresh stock and try again. The retry lives in a **separate bean** (`StockFacade`), because the conflict is detected at commit, after the `@Transactional` method has returned.
- **Atomic:** there is no read in Java at all. The condition `stock >= ?` is checked by the database while it updates, and the database itself serialises the updates on the row.

**What I had to change:** optimistic locking needs a `version` column on `product`. V1 and V2 are already applied and must never be edited, so I added **`V3__product_version.sql`** (`ALTER TABLE product ADD COLUMN version BIGINT DEFAULT 0 NOT NULL`) and `@Version` in `Product`.

**Good to know:**
- A JPQL bulk `update` bypasses the persistence context and does **not** increase `@Version`. `clearAutomatically = true` removes stale entities after it runs.
- The H2 numbers are close because everything runs in memory. On a real database each optimistic retry is another network round trip, so it gets clearly worse as more threads compete for the same row.

**When to use which:**
- **Atomic update:** the first choice when the whole rule fits in one SQL statement (decrement if enough).
- **Pessimistic lock:** read-check-write logic that is too complex for one statement, with **frequent** conflicts. Keep the transaction short, because others wait.
- **Optimistic lock:** **rare** conflicts (most edit forms). Needs a retry loop outside the transaction.

---

### 4. Bulk import of 50 000 products

`BulkImportService` reads a CSV (`sku;name;amount;currency;category;stock`). `BulkImportTest` generates 50 000 rows into a temp file and imports them three ways. A background thread samples the used heap every 5 ms to estimate the peak.

| Strategy | Time | Heap growth | Statements |
|---|---:|---:|---:|
| `StatelessSession` + `insertMultiple` (chunks of 500, `setJdbcBatchSize(500)`) | **0.9 s** | ~100–160 MB | 2 100 |
| `saveAll` in one `@Transactional`, no clear | 1.4 s | ~250–300 MB | – |
| `saveAll` + `flush()` + `clear()` every 500 | 2.5 s | ~160 MB | – |

```
INFO  BulkImportTest : stateless insertMultiple(500): 50000 rows in 0.9 s, 2100 statements, heap +102 MB
INFO  BulkImportTest : saveAll, one tx, no clear   : 50000 rows in 1.6 s, heap +296 MB
INFO  BulkImportTest : saveAll + flush/clear(500)  : 50000 rows in 2.2 s, heap +165 MB
```
All three are under the 10 s target. The heap numbers move between runs because the garbage collector runs at different moments.

**Why the differences:**
- **StatelessSession** has no persistence context: no first-level cache, no snapshots for dirty checking, no cascades. Each chunk of 500 goes straight to JDBC as one batch, then the objects can be garbage collected. The 2 100 statements are 1 000 `next value for product_seq` calls (allocationSize 50) plus about 100 batches.
- **`saveAll` without clear** keeps all 50 000 entities **and** their snapshots in memory until commit, which is why it uses the most heap. With 500 000 rows it would run out of memory.
- **`saveAll` + flush/clear** keeps memory flat in a normal session. It flushes 100 times instead of once, which is why it was the slowest here.

**When to use which:** StatelessSession for imports and exports where you only need plain inserts. Use flush/clear in a normal session when you still need cascades, entity listeners or `@Version` handling. Never use one big `saveAll` for large files.

**Logging trap:** with SQL DEBUG + bind TRACE logging on, 50 000 inserts write about 350 000 log lines, which takes longer than the import itself. `BulkImportTest` and `StockReservationTest` switch SQL logging off with `@SpringBootTest(properties = …)`.

---

### Acceptance checklist
- [x] The Criteria report combines any subset of five filters, joins tags only when needed, sorts from a whitelist and returns a record projection, using the generated metamodel.
- [x] The feed works with both Slice and keyset scrolling. Both SQL statements are shown above, with the explanation of the difference.
- [x] The 10-thread reservation test never oversells with the pessimistic lock. Optimistic and atomic-update variants are measured and compared.
- [x] 50 000 products import through StatelessSession in under 10 s (0.9 s), with measurements for all three strategies.
- [x] `README.md` has the ER diagram and the table of every repository method with its SQL.

### Test Result
```
./mvnw verify
[INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
```
