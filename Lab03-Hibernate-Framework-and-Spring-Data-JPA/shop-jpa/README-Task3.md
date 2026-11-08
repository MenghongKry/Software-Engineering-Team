## Task 3 – Boot-time validation of derived queries

Introduced a typo in `OrderRepository`: `findByCustomerEmaliAndStatusOrderByPlacedAtDesc`.
The project compiles, but the application refuses to start:

```
Caused by: org.springframework.data.repository.query.QueryCreationException: Cannot create query for method [OrderRepository.findByCustomerEmaliAndStatusOrderByPlacedAtDesc(java.lang.String,edu.itc.se.shop_jpa.domain.OrderStatus)]; No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
Caused by: org.springframework.data.core.PropertyReferenceException: No property 'emali' found for type 'Customer'; Did you mean 'email'; Traversed path: Order.customer
```

Spring Data parses every derived method name when the repository bean is created
and resolves each part against the entity model (`Order.customer` → `Customer.emali`).
An invalid property path is therefore a start-up failure, not a runtime error on first call.

## What I Did
I replaced hand-written data access with Spring Data repositories (`CustomerRepository`, `ProductRepository`, `OrderRepository`, `TagRepository`):

- **Derived queries:** `findByCustomerEmailAndStatusOrderByPlacedAtDesc`, `countByStatus`, `findFirstByCustomerIdOrderByPlacedAtDesc`, `existsBySku`, `findTop3ByCategoryOrderByPriceAmountDesc`, `findByTagsName`.
- **JPQL with `@Query`:** `findWithLinesByCustomerEmail` (fetch join of lines and products) and `countLinesByStatus` (aggregate).
- **Native SQL:** `findLowStock(int min)`.
- **Projections:** the interface `OrderSummary` for `/api/orders` and the record `ProductView` for `/api/products`.
- **Specifications:** `ProductSpecs` with `nameContains`, `inCategory`, `cheaperThan`, `inStock`, and `filter(...)`, which combines only the parameters that are present.
- **`DataSeeder`** (`@Profile("dev")`): 3 customers, 12 products with tags, and 30 orders, all saved in one `@Transactional` method.
- **Three endpoints.** Controllers only call services and never return entities. `@Transactional` is only on service methods.

The full HTTP transcript is in `api-session.md`.

## Interface Projection: the Nested Projection Pitfall
My first version of `findByStatus` was a plain derived query returning `Page<OrderSummary>` with a nested `CustomerName` interface. The SQL selected only three columns from `orders`, but it loaded the **whole customer**:

```
select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.id,c1_0.street,c1_0.city,c1_0.zip,c1_0.email,c1_0.name,c1_0.ship_street,c1_0.ship_city,c1_0.ship_zip from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id where o1_0.status=? order by o1_0.placed_at desc fetch first ? rows only
```

A nested interface projection in a derived query loads the associated entity completely. I fixed it with a JPQL query that uses aliases (`c.name as customerName`). `OrderSummary` gets `getCustomerName()` from the alias, and a `default` method `getCustomer()` rebuilds the nested object, so the JSON stays `{"customer": {"name": ...}}`. Now only the projected columns are selected:

```
select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.name from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id where o1_0.status=? order by 3 desc fetch first ? rows only
select count(o1_0.id) from orders o1_0 where o1_0.status=?
```

## Record Projection: Why `ProductView` Uses `@Query`
For a derived query, Spring matches the record's constructor parameters to entity properties **by name**. `amount` does not exist on `Product`, because it lives in the embedded `Money` as `price.amount`. So I used a constructor expression, `select new ...ProductView(p.id, p.sku, p.name, p.price.amount, p.stock)`, with an explicit `countQuery` for paging.

## Specifications
`/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true` became exactly four `AND` conditions:

```
where lower(p1_0.name) like ? escape '' and p1_0.category=? and p1_0.amount<=? and p1_0.stock>?
```

When no filter is given, no `WHERE` clause is generated. I collect only the non-null specifications in a list before calling `Specification.allOf`, because Spring Data JPA 4 is stricter about `null` arguments.

## Fetch Join and open-in-view = false
`/api/customers/{email}/orders` calls `findWithLinesByCustomerEmail` inside a `@Transactional(readOnly = true)` service method and maps to `OrderDto` before returning. One `select distinct ... join order_line ... join product` loads everything, so no extra `SELECT`s run while mapping. `join fetch` is an inner join, so a customer's order with no lines would not appear (`left join fetch` would include it).

## Testing Lazy Loading
`Order.getLines()` returns `Collections.unmodifiableList(lines)`. `Hibernate.isInitialized(order.getLines())` therefore always returns `true`, because it sees the wrapper, not Hibernate's `PersistentBag`. `OrderRepositoryTest` uses `PersistenceUnitUtil.isLoaded(order, "lines")` instead, which checks the field itself: `true` after the fetch-join query, `false` after a plain `findById` on a cleared persistence context.

## Test Result
`OrderRepositoryTest` (5 tests):

```
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.079 s -- in edu.itc.se.shop_jpa.OrderRepositoryTest
```

Lazy vs. fetch join, as seen in the test log. A plain `findById` runs two statements:
```
select o1_0.id,o1_0.customer_id,o1_0.placed_at,o1_0.status,o1_0.version from orders o1_0 where o1_0.id=?
select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.amount,l1_0.currency from order_line l1_0 where l1_0.order_id=?
```
`findWithLinesByCustomerEmail` runs one statement (`select distinct ... join order_line ... join product ...`).
```