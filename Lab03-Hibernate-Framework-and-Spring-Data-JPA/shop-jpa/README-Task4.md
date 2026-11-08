## Part 4: Entity states, dirty checking, transactions and optimistic locking

### Files Changes
- **Service:** `OrderService.java`, `OrderFacade.java`
- **Tests:** `OrderServiceTest.java`, `ProductDetachedEntityTest.java`, `TagFlushOrderTest.java`
- **Test configuration:** `src/test/resources/application-test.yml`

### Detached product (`ProductDetachedEntityTest`)

The merge part of the test log shows Hibernate loading the row, then updating it with stock `99`:

```
15:41:02.051 DEBUG [none] org.hibernate.SQL : select p1_0.id,p1_0.category,p1_0.name,p1_0.amount,p1_0.currency,p1_0.sku,p1_0.stock from product p1_0 where p1_0.id=?
15:41:02.054 DEBUG [none] org.hibernate.SQL : update product set category=?,name=?,amount=?,currency=?,sku=?,stock=? where id=?
15:41:02.055 TRACE [none] org.hibernate.orm.jdbc.bind : binding parameter (6:INTEGER) <- [99]
```

### Read-only transaction trap (`OrderServiceTest`)

The read-only test changes an order in memory, but Hibernate does not flush that change. Reloading the order still shows `NEW`, and the test observes zero entity updates.

### Flush order (`TagFlushOrderTest`)

Deleting a tag and adding another with the same unique name before flushing can fail: Hibernate sends the insert before the delete. Flushing the delete first fixes it:

```java
tags.delete(saleTag);
tags.flush();
tags.save(new Tag("sale"));
```

`flush()` sends the pending delete to the database; it does not commit the transaction.

### Optimistic-Locking Logs

With the `test` profile enabled, the concurrent status-change test shows both service transactions starting, both updates checking the version, and one transaction rolling back after its stale update:

```
15:28:19.382 TRACE [none] o.s.t.i.TransactionInterceptor : Getting transaction for [edu.itc.se.shop_jpa.service.OrderService.changeStatus]
15:28:19.382 TRACE [none] o.s.t.i.TransactionInterceptor : Getting transaction for [edu.itc.se.shop_jpa.service.OrderService.changeStatus]
15:28:19.386 DEBUG [none] org.hibernate.SQL : update orders set customer_id=?,placed_at=?,status=?,version=? where id=? and version=?
15:28:19.386 DEBUG [none] org.hibernate.SQL : update orders set customer_id=?,placed_at=?,status=?,version=? where id=? and version=?
15:28:19.391 DEBUG [none] o.s.orm.jpa.JpaTransactionManager : Initiating transaction rollback after commit exception
15:28:19.393 DEBUG [none] o.s.orm.jpa.JpaTransactionManager : Rolling back JPA transaction on EntityManager [SessionImpl(1278399422<open>)]
```
