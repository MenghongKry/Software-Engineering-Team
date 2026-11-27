# shop-secure — Lab-05: Security in Java Web Applications

Spring Boot 4.1.1 · Spring Security 7.1 · JDK 25 · Maven 3.9

## How to run

```bash
cp .env.example .env                       # fill in values locally (.env is git-ignored)
export JWT_SECRET=$(openssl rand -base64 48)
export SHOP_SEED_ADMIN_PASSWORD='...' SHOP_SEED_CUSTOMER_PASSWORD='...'
./mvnw spring-boot:run                     # http://localhost:8080
./mvnw -q verify                           # build + all tests
```

## Environment variables

| Variable | Needed from | Purpose |
|---|---|---|
| `JWT_SECRET` | Task 4 | HS256 signing key, at least 32 bytes (`openssl rand -base64 48`). Start-up fails without it. |
| `SHOP_SEED_ADMIN_PASSWORD` | Task 2/5 | Password of the seeded `admin` account. |
| `SHOP_SEED_CUSTOMER_PASSWORD` | Task 2/5 | Password of the seeded `alice` account. |

## Task 1 — Default security (auto-configuration)

_TODO: quote the `Using generated security password: …` line here._

_TODO: explain which auto-configuration produced the default behaviour
(`SecurityAutoConfiguration` / `SpringBootWebSecurityConfiguration` → default filter chain,
`UserDetailsServiceAutoConfiguration` → in-memory `user` with a generated password). Use `--debug` (conditions report)._

## Task 2 — Users and passwords

_TODO_

## Task 3 — Roles and method security

_TODO_

## Task 4 — JWT login

_TODO_

## Task 5 — Hardening

_TODO (see `docs/csrf.md`)_

## Transcripts

All curl transcripts, the decoded JWT, header block and XSS screenshots: [security-session.md](security-session.md).
