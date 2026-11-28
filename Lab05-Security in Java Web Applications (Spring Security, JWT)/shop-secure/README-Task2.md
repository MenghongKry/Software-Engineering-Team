README
· MD
shop-secure (Lab-05, Task 2: Users and passwords in the database)

Task 1 (default security + first SecurityFilterChain) was done by my teammates, so its part of the README is theirs. This file only covers Task 2.

What I did in this task

Before this task, the app had two fake users (admin and alice) stored in memory with {noop} passwords. That was only for testing. In Task 2 I replaced them with real users stored in the database, with hashed passwords.

Here is what I added:

domain/Role and domain/UserAccount: the user entity (id, username, passwordHash, enabled, roles). Roles are stored in a separate user_roles table and loaded eagerly. I made toString() skip the hash so it never shows up in logs.
repository/UserAccountRepository: findByUsername and existsByUsername.
config/PasswordConfig: gives Spring a PasswordEncoder made by PasswordEncoderFactories.createDelegatingPasswordEncoder().
security/JpaUserDetailsService: loads the user from the DB so Spring Security can check the login. I deleted the old InMemoryUserDetailsManager.
service/AccountService + UsernameTakenException: register(...) hashes the password and saves the user. If the username already exists, it throws the exception.
web/RegisterController + web/dto/RegisterRequest: POST /api/register with validation. Success returns 201 with only the username, never the password.
web/MeController: GET /api/me, just to check who I'm logged in as.
A CommandLineRunner in ShopSecureApplication that creates admin and alice if they don't exist yet. The passwords come from application.yml, not from the Java code.

I also changed ApiExceptionHandler (409 for a taken username, and validation errors as a list of {field, message}) and SecurityConfig (open POST /api/register, remove the temporary users).

How the password hashing works

I used the delegating password encoder. It saves passwords like {bcrypt}$2a$10$.... The {bcrypt} prefix tells Spring which algorithm was used, so later we can change to a newer algorithm and old passwords still work. The same password gives a different hash each time because bcrypt adds a random salt. That is normal.

Password and username rules
Username: 3 to 40 characters, only a-z, 0-9, _ and .
Password: 12 to 72 characters, with at least one uppercase letter, one lowercase letter and one digit
The 72 limit is there because bcrypt only reads the first 72 bytes.

Self-registered users only get the CUSTOMER role, so nobody can make themselves admin.

How to test it

Start the app with ./mvnw spring-boot:run, then:

bash
# 1. weak password -> 400 with the errors list
curl -i -X POST localhost:8080/api/register -H 'Content-Type: application/json' -d '{"username":"bob","password":"short"}'

# 2. good password -> 201
curl -i -X POST localhost:8080/api/register -H 'Content-Type: application/json' -d '{"username":"bob","password":"Correct-Horse-9"}'

# 3. same username again -> 409
curl -i -X POST localhost:8080/api/register -H 'Content-Type: application/json' -d '{"username":"bob","password":"Correct-Horse-9"}'

# 4. login with the new user -> 200
curl -i -u bob:Correct-Horse-9 localhost:8080/api/me

# 5. wrong password -> 401
curl -i -u bob:wrongpassword localhost:8080/api/me

All the outputs are saved in security-session.md.

One row from the users table
<paste one USER ROW line here, for example:>
1 | admin | {bcrypt}$2a$10$................................................ | true
What was hard for me
Everything returned 401 and I didn't know why. I first thought my SecurityConfig was wrong. The real problem was that an old copy of the app was still running on port 8080, so the new one could not start and my curl commands were talking to the old code. I found it in the log: Port 8080 was already in use. Now I check the log first, and I kill the old process with lsof -i :8080 and kill.
Two exception handlers for the same exception. I pasted a second MethodArgumentNotValidException handler into ApiExceptionHandler while the old one was still there. Spring does not allow that, so I merged them into one.
The order of the rules in SecurityConfig matters. /api/register must be allowed before the /api/** authenticated rule, because the first matching rule wins.
SELECT * FROM users; in the terminal does not work (it is SQL, not a shell command). I printed the rows in the startup log instead of using the H2 console.
What I learned
A password should never be stored as plain text. Bcrypt is slow on purpose, so guessing passwords is harder.
Spring Security finds my UserDetailsService bean automatically once I define it, so I did not need to wire it by hand.
Validation errors are easier to use as a list of fields than as one long string.
Seed passwords belong in config, not in the Java code.