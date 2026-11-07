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