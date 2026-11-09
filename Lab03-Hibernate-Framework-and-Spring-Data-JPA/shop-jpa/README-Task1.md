# Task 1 – Hibernate Framework and Spring Data JPA

## 1. Objective

The objective of Task 1 is to understand how to use Hibernate and Spring Data JPA to map Java objects to database tables. It also introduces database schema management using Flyway and verifies that the database structure matches the Java entity mappings.

## 2. Database Schema with Flyway

I used Flyway to manage the database schema through the migration file:

`src/main/resources/db/migration/V1__init.sql`

This migration is responsible for creating:

- Six database tables.
- Five database sequences.

Flyway automatically executes the migration when the application starts, creating the required database structure.

## 3. Java Entity Mapping

I used Java classes and JPA annotations to map application objects to database structures.

### Money.java and Address.java

Both `Money` and `Address` are defined as `@Embeddable` records.

- `Money` represents a monetary value and its currency.
- `Address` represents address information.
- `@Embeddable` allows their fields to be included in the database table of another entity.

### Enum Mapping

Both enum fields use:

```java
@Enumerated(EnumType.STRING)
```

This stores enum values as readable text in the database instead of numerical positions.

### ID Management

Entity IDs are managed by Hibernate using generated values and database sequences. No manual `setId()` methods are used.

## 4. Hibernate Schema Validation

In the application configuration, I used:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This tells Hibernate to validate the existing database schema against the entity mappings.

Hibernate checks whether the database tables and columns match the Java entity definitions. It does not automatically create or update the tables.

## 5. Deliberate Schema Mismatches

As part of the exercise, I reproduced and documented two intentional schema mismatches.

### Mismatch 1: Missing Column

I reproduced a situation where a column required by an entity was missing from the database schema.

When the application started, Hibernate detected that the database structure did not match the entity mapping and reported a schema validation error.

I renamed `sku` to `code` in `V1__init.sql` only (the entity still says `sku`). The exact error:

```
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing column [sku] in table [product]
```

**What I learned:** The database schema must contain all required columns defined by the entity mappings.

### Mismatch 2: Sequence Increment

I reproduced a mismatch between the database sequence increment and the sequence configuration expected by Hibernate.

I changed `CREATE SEQUENCE product_seq ... INCREMENT BY 50` to `INCREMENT BY 1` (the entity still has `allocationSize = 50`). The exact error:

```
Caused by: org.hibernate.MappingException: The increment size of the [product_seq] sequence is set to [50] in the entity mapping but the mapped database sequence increment size is [1]
```

This demonstrated that the sequence configuration in the database and Java entity mapping must be consistent.

**What I learned:** Sequence settings, including the increment size, must match the Hibernate configuration to avoid validation problems.

## 6. Product Mapping Test

I used `ProductMappingTest` to verify that the Product entity can be saved and retrieved correctly using the Flyway-created database schema.

The test also verifies that the enum value is stored as text in the database.

This confirms that the entity mapping and database schema work together as expected.

## 7. Conclusion

In Task 1, I learned how Hibernate, JPA, and Flyway work together in a Spring Boot application.

I practiced mapping Java classes to database tables, using embedded records, mapping enums as strings, managing IDs with sequences, and validating the database schema.

I also learned how schema mismatches can cause application startup errors and how to identify the problems through Hibernate validation.

**Final verification:** The task is considered complete when all acceptance checklist requirements are satisfied and `ProductMappingTest` passes successfully.