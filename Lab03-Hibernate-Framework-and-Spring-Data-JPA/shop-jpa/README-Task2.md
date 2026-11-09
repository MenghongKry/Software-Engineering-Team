## Part 2: Associations, Cascading and Entity Identity

### What I Did
I made `Order` the owner of its lines. `Order.lines` uses `mappedBy = "order"`, `cascade = ALL` and `orphanRemoval = true`, and `OrderLine.order` is the owning side. The helpers `addLine` and `removeLine` keep both sides in sync. I also added the many-to-many between `Product` and `Tag` through `product_tag`, with `Set` collections on both sides. Every entity got hand-written `equals`, `hashCode` and `toString`.

### What the SQL Log Showed
When I persisted an order with two lines, Hibernate ran one insert into `orders` and two inserts into `order_line`, with no `update` afterwards:

```
DEBUG [none] org.hibernate.SQL : insert into orders (customer_id,placed_at,status,version,id) values (?,?,?,?,?)
DEBUG [none] org.hibernate.SQL : insert into order_line (order_id,product_id,quantity,amount,currency,id) values (?,?,?,?,?,?)
DEBUG [none] org.hibernate.SQL : insert into order_line (order_id,product_id,quantity,amount,currency,id) values (?,?,?,?,?,?)
```

When I removed one line from a loaded order, orphan removal ran a single delete:

```
DEBUG [none] org.hibernate.SQL : delete from order_line where id=?
```

After that I removed the whole order and the remaining line was deleted too (cascade REMOVE), so the line count went back to 0.

### The toString Pitfall
I tried it on purpose. I temporarily put `order` in `OrderLine.toString()` and `lines` in `Order.toString()`, then printed an order outside a transaction. This is the error I got:

```
org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'edu.itc.se.shop_jpa.domain.Order.lines' with key '1' (no session)
        at org.hibernate.collection.spi.AbstractPersistentCollection.throwLazyInitializationException(AbstractPersistentCollection.java:664)
        at org.hibernate.collection.spi.AbstractPersistentCollection.withTemporarySessionIfNeeded(AbstractPersistentCollection.java:239)
        at org.hibernate.collection.spi.AbstractPersistentCollection.initialize(AbstractPersistentCollection.java:624)
        at org.hibernate.collection.spi.AbstractPersistentCollection.read(AbstractPersistentCollection.java:149)
        at org.hibernate.collection.spi.PersistentBag.toString(PersistentBag.java:637)
        at java.base/java.util.Formatter$FormatSpecifier.printString(Formatter.java:3292)
        at java.base/java.util.Formatter$FormatSpecifier.print(Formatter.java:3170)
        at java.base/java.util.Formatter.format(Formatter.java:2761)
        at java.base/java.util.Formatter.format(Formatter.java:2698)
        at java.base/java.lang.String.formatted(String.java:4517)
        at edu.itc.se.shop_jpa.domain.Order.toString(Order.java:86)
        at java.base/java.lang.String.valueOf(String.java:4530)
        at org.apache.maven.surefire.api.report.ConsoleOutputCapture$ForwardingPrintStream.println(ConsoleOutputCapture.java:110)
        at edu.itc.se.shop_jpa.ToStringPitfallTest.pitfall(ToStringPitfallTest.java:32)
```

The order was loaded in a transaction that had already ended, so `lines` was still lazy and Hibernate had no session to load it. If the transaction had still been open, `Order.toString()` would call `OrderLine.toString()`, which prints `order`, which prints `lines` again. That loop ends in a `StackOverflowError`. I reverted both methods afterwards, and now `toString` only prints simple fields like `id` and `status`.

### Why equals and hashCode Look Like This
`equals` compares class and a non-null `id`. `hashCode` returns one constant per class. The id is null before persist and gets set after, so a hash based on the id would change while the object sits in a `HashSet`. With the constant hash, `set.contains(product)` is still true after persist and flush. `EntityIdentityTest` checks this.

### Something That Confused Me
optional: write one honest sentence, for example that `orphanRemoval` and the `delete` only showed up in the log after `flush()`, or that the lazy-loading error made more sense once I saw it myself

### Test Result
`OrderCascadeTest` (3 tests) and `EntityIdentityTest` (2 tests) pass:

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```