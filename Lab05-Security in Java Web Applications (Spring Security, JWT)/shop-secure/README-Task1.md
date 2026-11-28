# Task 1 – Default Security and Your First Filter Chain

## 1. Project Overview

This project demonstrates how Spring Security protects a REST API using HTTP Basic Authentication, role-based authorization, and a custom SecurityFilterChain.

The application provides a public product catalogue while requiring authentication and appropriate permissions for other operations.

### Technologies Used

- Java
- Spring Boot
- Spring Security
- Spring Web
- Spring Validation
- Maven

---

## 2. Product REST API

The application implements a product management REST API.

### Product Structure

Each product contains:

| Field | Type |
|---|---|
| id | long |
| name | String |
| price | BigDecimal |
| stock | int |

### Available Endpoints

| HTTP Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get a product by ID |
| POST | `/api/products` | Create a product |
| DELETE | `/api/products/{id}` | Delete a product |

### Initial Products

Three products are seeded using CommandLineRunner.

| ID | Name | Price | Stock |
|---|---|---:|---:|
| 1 | USB-C cable | 7.90 | 25 |
| 2 | Keyboard | 35.00 | 10 |
| 3 | Mouse | 19.90 | 15 |

---

## 3. Default Spring Security Behavior

Before implementing a custom SecurityFilterChain, the application was tested using Spring Security's default configuration.

### 3.1 Security Auto-Configuration

Spring Boot automatically configures Spring Security through `UserDetailsServiceAutoConfiguration`.

When no custom UserDetailsService or AuthenticationManager is provided, Spring Boot creates a default user named `user` and generates a password that is printed in the application startup logs.

### 3.2 Generated Security Password

Actual generated password log:

```text
PASTE YOUR ACTUAL GENERATED PASSWORD LOG HERE
```

### 3.3 Default Security Tests

**Test 1: Anonymous GET**

Command:

```powershell
curl.exe -i http://localhost:8080/api/products
```

Expected result:

```http
HTTP/1.1 401 Unauthorized
```

Actual result:

```text
PASTE YOUR ACTUAL ANONYMOUS GET RESULT HERE
```

**Test 2: Authenticated GET**

Command:

```powershell
curl.exe -i -u user:YOUR_GENERATED_PASSWORD http://localhost:8080/api/products
```

Expected result:

```http
HTTP/1.1 200 OK
```

Actual result:

```text
PASTE YOUR ACTUAL AUTHENTICATED GET RESULT HERE
```

These tests demonstrate how Spring Security automatically protects endpoints before an explicit SecurityFilterChain is configured.

---

## 4. Custom SecurityFilterChain Configuration

A custom SecurityFilterChain was implemented in `SecurityConfig.java` using the lambda DSL.

### 4.1 Authorization Rules

| Request | Permission |
|---|---|
| GET `/api/products/**` | `permitAll()` |
| DELETE `/api/products/**` | `hasRole("ADMIN")` |
| Other `/api/**` requests | `authenticated()` |
| Any other request | `denyAll()` |

### 4.2 HTTP Basic Authentication

HTTP Basic authentication is enabled using:

```java
.httpBasic(Customizer.withDefaults())
```

Form login is disabled:

```java
.formLogin(form -> form.disable())
```

### 4.3 Stateless Session Management

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(
        SessionCreationPolicy.STATELESS
    )
)
```

The API does not maintain HTTP sessions for authentication.

### 4.4 CSRF Configuration

```java
.csrf(csrf -> csrf.disable())
```

CSRF protection is disabled because this API uses stateless HTTP Basic/Bearer authentication and does not rely on cookie-based browser credentials.

---

## 5. In-Memory Users

Two temporary users are configured using `InMemoryUserDetailsManager`.

| Username | Password | Role |
|---|---|---|
| admin | admin | ADMIN |
| alice | alice | CUSTOMER |

### User Configuration

```java
User.withUsername("admin")
    .password("{noop}admin")
    .roles("ADMIN")
    .build();

User.withUsername("alice")
    .password("{noop}alice")
    .roles("CUSTOMER")
    .build();
```

`{noop}` indicates that passwords are stored without encoding for this temporary development exercise.

These temporary users will be replaced in Task 2.

---

## 6. Exception Handling

Custom exception handling is implemented to return consistent JSON responses.

### 6.1 HTTP 401 – Unauthorized

Returned when an anonymous user attempts to access a protected endpoint.

```json
{
    "type": "about:blank",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Authentication required"
}
```

### 6.2 HTTP 403 – Forbidden

Returned when an authenticated user attempts an operation without sufficient permissions.

```json
{
    "type": "about:blank",
    "title": "Forbidden",
    "status": 403,
    "detail": "Access denied"
}
```

Both responses use:

```text
Content-Type: application/problem+json
```

The responses are configured using:

- `authenticationEntryPoint` for HTTP 401.
- `accessDeniedHandler` for HTTP 403.

### 6.3 Validation and Not Found

`ApiExceptionHandler` handles:

- HTTP 400: Invalid product input.
- HTTP 404: Product not found.

Product validation uses Jakarta Validation annotations such as `@NotBlank`, `@NotNull`, `@DecimalMin`, and `@Min`.

---

## 7. Security Testing and Results

Four security tests were performed using PowerShell and `curl.exe`.

### Test 1: Anonymous GET

Command:

```powershell
curl.exe -i http://localhost:8080/api/products
```

Actual result:

```http
HTTP/1.1 200
Content-Type: application/json
```

Result: PASS

Anonymous users can access the public product catalogue.

### Test 2: Anonymous POST

Command:

```powershell
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{"id":0,"name":"Mouse","price":19.9,"stock":5}'
```

Actual result:

```http
HTTP/1.1 401
Content-Type: application/problem+json;charset=ISO-8859-1
```

Response:

```json
{
    "type": "about:blank",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Authentication required"
}
```

Result: PASS

Anonymous users cannot create products because authentication is required.

### Test 3: CUSTOMER DELETE

Command:

```powershell
curl.exe -i -u alice:alice -X DELETE http://localhost:8080/api/products/1
```

Actual result:

```http
HTTP/1.1 403
Content-Type: application/problem+json;charset=ISO-8859-1
```

Response:

```json
{
    "type": "about:blank",
    "title": "Forbidden",
    "status": 403,
    "detail": "Access denied"
}
```

Result: PASS

Alice is authenticated but does not have ADMIN permission.

### Test 4: ADMIN DELETE

Command:

```powershell
curl.exe -i -u admin:admin -X DELETE http://localhost:8080/api/products/1
```

Actual result:

```http
HTTP/1.1 204
```

Result: PASS

Admin successfully deleted product ID 1.

---

## 8. Test Summary

| Test | Expected | Actual | Status |
|---|---:|---:|---|
| Anonymous GET | 200 | 200 | PASS |
| Anonymous POST | 401 | 401 | PASS |
| CUSTOMER DELETE | 403 | 403 | PASS |
| ADMIN DELETE | 204 | 204 | PASS |

All four custom security tests passed successfully.

---

## 9. Conclusion

In this task, I implemented a secure REST API using Spring Security.

I learned how Spring Boot provides default security through auto-configuration and how to replace it with an explicit SecurityFilterChain.

I also implemented HTTP Basic authentication, role-based authorization, stateless session management, custom 401 and 403 responses, and validation error handling.

The testing results confirmed that public users, authenticated customers, and administrators receive different access permissions according to the configured security rules.

---

