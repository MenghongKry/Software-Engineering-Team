# Lab-02 · Spring Framework

Spring Boot 4.1.1 · Java 25 · Maven wrapper `./mvnw`

## Task 2 · Beans and dependency injection

All new code is in `shop/src/main/java/edu/itc/se/shop/`:

| File | Role |
|---|---|
| `service/Notifier.java` | interface with one method `send(to, text)` |
| `service/EmailNotifier.java` | `@Service @Primary @Profile("!dev")`: the default notifier |
| `service/SmsNotifier.java` | `@Service`: used only when asked for by name |
| `service/ConsoleNotifier.java` | `@Service @Primary @Profile("dev")`: prints to the console in dev |
| `service/WelcomeService.java` | gets two `Notifier`s and a `Clock` through its constructor |
| `service/ReportBuilder.java` | `@Scope("prototype")` bean |
| `config/AppConfig.java` | `@Bean Clock clock()` |
| `ContainerTour.java` | `CommandLineRunner` that looks inside the container at startup |

### 1. Constructor injection, `@Primary` and `@Qualifier`

```java
@Service
public class WelcomeService {
    public WelcomeService(Notifier defaultNotifier,
                          @Qualifier("smsNotifier") Notifier urgentNotifier,
                          Clock clock) { ... }
}
```

We never write `new WelcomeService(...)`. Spring sees the constructor, looks for beans that fit each parameter and passes them in.

- There are several `Notifier` beans, so for the first parameter Spring takes the one marked **`@Primary`**.
- The second parameter has **`@Qualifier("smsNotifier")`**, so Spring takes the bean with exactly that name (the class name with a small first letter).
- `Clock` is not one of our classes, so we cannot put `@Service` on it. Instead `AppConfig` has a **`@Bean`** method that returns it.

### 2. Lifecycle callbacks

Each notifier has `@PostConstruct` (runs after Spring has created and injected the bean) and `@PreDestroy` (runs when the app shuts down). That is why every run below starts with `... ready` and ends with `... closing`, closing in the reverse order.

### 3. Run without a profile

```
$ java -jar target/shop-0.0.1-SNAPSHOT.jar
INFO ShopApplication : No active profile set, falling back to 1 default profile: "default"
INFO EmailNotifier   : EmailNotifier ready
INFO SmsNotifier     : SmsNotifier ready
INFO ContainerTour   : 321 beans; notifiers = [emailNotifier, smsNotifier]
INFO EmailNotifier   : EMAIL to=ann text=Welcome ann! It is 22:35:06
INFO SmsNotifier     : SMS to=bob text=Welcome bob! It is 22:35:06
INFO ContainerTour   : prototype instances differ: true (1263085541 vs 1281205497)
INFO SmsNotifier     : SmsNotifier closing
INFO EmailNotifier   : EmailNotifier closing
```

- `ConsoleNotifier` is missing because its `@Profile("dev")` is not active.
- The normal welcome for ann went by email (the `@Primary` bean), the urgent one for bob by SMS (the `@Qualifier`).
- **321 beans**: only 8 of them are ours. The rest are created by Spring Boot's auto-configuration (Tomcat, Jackson, DataSource, Actuator…).
- **Prototype**: asking `ObjectProvider<ReportBuilder>` twice gives two different objects. A normal (singleton) bean would give the same object every time.

### 4. Run with `--spring.profiles.active=dev`: the error

Before the fix, `EmailNotifier` had no `@Profile`. In dev it was created **together with** `ConsoleNotifier`, and both are `@Primary`:

```
$ java -jar target/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
INFO  ShopApplication : The following 1 profile is active: "dev"
ERROR SpringApplication : Application run failed
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'containerTour' ...
Caused by: org.springframework.beans.factory.NoUniqueBeanDefinitionException: No qualifying bean of type
'edu.itc.se.shop.service.Notifier' available: more than one 'primary' bean found among candidates:
[consoleNotifier, emailNotifier, smsNotifier]
```

`@Primary` means "choose me when there are several", so two primaries put Spring back where it started: it cannot choose, and the app does not start.

### 5. The fix: `@Profile("!dev")` on `EmailNotifier`

```java
@Service
@Primary
@Profile("!dev")   // "every profile except dev"
public class EmailNotifier implements Notifier { ... }
```

Now in dev only one primary exists:

```
$ java -jar target/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
INFO ShopApplication : The following 1 profile is active: "dev"
INFO ConsoleNotifier : ConsoleNotifier ready
INFO SmsNotifier     : SmsNotifier ready
INFO ContainerTour   : 321 beans; notifiers = [consoleNotifier, smsNotifier]
CONSOLE to=ann text=Welcome ann! It is 22:35:09
INFO SmsNotifier     : SMS to=bob text=Welcome bob! It is 22:35:09
INFO ContainerTour   : prototype instances differ: true (649827042 vs 412121842)
INFO SmsNotifier     : SmsNotifier closing
INFO ConsoleNotifier : ConsoleNotifier closing
```

`WelcomeService` did not change at all, yet in dev it now sends through the console. Which class gets injected is decided by configuration, not by the code that uses it. That is the point of dependency injection. (The bean count stays 321 because one notifier was swapped for another.)

### 6. Plain JUnit test

`shop/src/test/java/edu/itc/se/shop/service/WelcomeServiceTest.java` starts **no Spring at all**. It calls the constructor itself with two fake notifiers (lambdas that remember what they got) and a fixed `Clock`:

```java
private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T09:15:02Z"), ZoneOffset.UTC);
private final WelcomeService service = new WelcomeService(
        (to, text) -> emails.add(to + ":" + text),
        (to, text) -> sms.add(to + ":" + text),
        clock);
```

Because the dependencies come in through the constructor, a test can hand in fakes. The fixed clock makes the time always `09:15:02`, so the expected text can be checked exactly.

```
$ ./mvnw test
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.se.shop.ShopApplicationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in edu.itc.se.shop.service.WelcomeServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The Spring test takes about 1.5 s (it starts the whole container), the plain test about 0.01 s.

