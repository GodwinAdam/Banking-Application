# Code Walkthrough

This document explains **every significant file** in the project, section
by section: what each annotation and line does, and *why* it's there. Read
it alongside the actual source files in `src/main/java/com/bankingapp/`.

If you're new to Spring Boot, read this in order — later sections build on
concepts explained earlier (e.g. the Entity section explains JPA basics
that the Repository section assumes you already understand).

## Table of contents

1. [BankingApplication.java — the entry point](#1-bankingapplicationjava--the-entry-point)
2. [Entities — how Java classes become database tables](#2-entities--how-java-classes-become-database-tables)
3. [Enums](#3-enums)
4. [Repositories — Spring Data JPA](#4-repositories--spring-data-jpa)
5. [DTOs — why not just return entities?](#5-dtos--why-not-just-return-entities)
6. [Exceptions and GlobalExceptionHandler](#6-exceptions-and-globalexceptionhandler)
7. [Services — where business logic lives](#7-services--where-business-logic-lives)
8. [Controllers — the HTTP layer](#8-controllers--the-http-layer)
9. [Configuration files](#9-configuration-files)

---

## 1. `BankingApplication.java` — the entry point

```java
@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
```

- `@SpringBootApplication` is shorthand for three annotations stacked
  together:
  - `@Configuration` — this class is allowed to declare `@Bean` methods.
  - `@EnableAutoConfiguration` — Spring Boot inspects what's on the
    classpath (e.g. "H2 driver present, no `DataSource` bean defined") and
    configures sensible defaults automatically. This is *why* our project
    needs almost no manual wiring.
  - `@ComponentScan` — Spring scans this class's package
    (`com.bankingapp`) and every sub-package for `@Component`, `@Service`,
    `@Repository`, `@RestController`, `@Configuration` classes and
    registers them as **beans** — objects whose lifecycle (creation,
    dependency injection, destruction) Spring manages for you.
- `SpringApplication.run(...)` creates the **ApplicationContext** (the
  container holding every bean), starts the embedded Tomcat server, and
  blocks the main thread so the JVM stays alive to serve requests.

Why does package placement matter? Because `@ComponentScan` only scans
*this class's package and below*. Every class we write lives under
`com.bankingapp.*`, so nothing needs an explicit `@ComponentScan(basePackages=...)`.

---

## 2. Entities — how Java classes become database tables

Take `Account.java` as the representative example (`Customer.java` and
`Transaction.java` follow the exact same pattern):

```java
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    ...
}
```

Line by line:

- **`@Entity`** — tells Hibernate (the JPA implementation Spring Boot
  wires up for us) that this class maps to a database table. Without it,
  the class is just a plain Java object Hibernate never sees.
- **`@Table(name = "accounts")`** — the actual table name. If you omit
  this, Hibernate defaults to the class name, but naming it explicitly
  avoids surprises if the class is ever renamed.
- **`@Id`** — marks `id` as the primary key column.
- **`@GeneratedValue(strategy = GenerationType.IDENTITY)`** — the database
  itself generates this value on insert (H2/MySQL `AUTO_INCREMENT`,
  Postgres `SERIAL`/identity column). We never set `id` manually in code.
- **`@Column(...)`** — customizes the generated column: its name,
  `nullable` (→ `NOT NULL` constraint), `unique` (→ `UNIQUE` constraint),
  `length` (→ `VARCHAR(12)`), or `precision`/`scale` for exact decimal
  storage.
- **`@Enumerated(EnumType.STRING)`** — stores the enum's *name* ("SAVINGS")
  in the column, not its ordinal position (0, 1, 2...). This matters: if
  someone later reorders the `AccountType` enum constants, `ORDINAL`
  storage would silently corrupt every existing row's meaning, while
  `STRING` storage is immune to that.
- **`@ManyToOne(fetch = FetchType.LAZY)`** + **`@JoinColumn(name =
  "customer_id")`** — declares the foreign key: many `Account` rows point
  to one `Customer` row, via a `customer_id` column. `FetchType.LAZY`
  means loading an `Account` does **not** automatically also load its
  `Customer` from the database — that only happens the moment
  `.getCustomer()` is actually called. This avoids pulling in unnecessary
  data every time you just need an account's balance.
- **`BigDecimal balance`** (never `double`/`float`) — this is the single
  most important line in the whole codebase for a *banking* app. Binary
  floating-point types cannot exactly represent decimal fractions like
  `0.1`; those tiny errors compound across thousands of transactions into
  real, wrong money. `BigDecimal` with `precision=19, scale=2` stores exact
  decimal values.
- **Why `compareTo`, not `==` or `.equals()`, for balance checks?** See the
  `TransactionServiceImpl` section below — it's explained there where it's
  actually used.
- **Protected no-arg constructor** (`protected Account() {}`) — Hibernate
  needs *some* way to instantiate the class via reflection before
  populating its fields from a database row. It's `protected` rather than
  `public` so application code is nudged toward using the real,
  validating constructor instead.
- **`@PrePersist`** — a lifecycle callback Hibernate invokes immediately
  before the `INSERT` statement runs. Used here purely as a safety net to
  make sure `createdAt` is never accidentally null.
- **`getBalance()` / `setBalance(...)`** — plain getters/setters. Both
  Hibernate (to read/write column values) and Jackson (to serialize to
  JSON, if an entity were ever directly returned) rely on this convention
  rather than direct field access.

**`Customer.java`** additionally has:

```java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Account> accounts = new ArrayList<>();
```

- **`mappedBy = "customer"`** — this is the "inverse" side of the
  relationship. The actual foreign key column (`customer_id`) lives on the
  `Account` table (see `@JoinColumn` above); `Customer` doesn't own a
  column for this, it's just a convenient Java-side view of "which
  accounts point at me".
- **`cascade = CascadeType.ALL`** — saving or deleting a `Customer` also
  saves/deletes everything in its `accounts` list.
- **`orphanRemoval = true`** — if an `Account` is ever removed from this
  list in code, Hibernate deletes that row too, rather than leaving it
  behind with a now-meaningless foreign key.

**`Transaction.java`** is intentionally **append-only** — the service
layer never calls `UPDATE` or `DELETE` on a `Transaction` row once
written. That immutability is exactly what makes it trustworthy as an
audit trail, the same as a real bank statement line.

---

## 3. Enums

```java
public enum AccountType { SAVINGS, CURRENT }

public enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN }
```

Using enums instead of raw `String`s for these fields means:
- Typos like `"SAVING"` vs `"SAVINGS"` are caught by the **compiler**, not
  discovered as a runtime data-quality bug months later.
- Jackson automatically validates incoming JSON — sending
  `"accountType": "CHECKING"` (not a real constant) fails immediately with
  a clear `400 Bad Request`, before it ever reaches business logic.

`TRANSFER_OUT`/`TRANSFER_IN` are separate constants (not one generic
`TRANSFER`) because a transfer writes **two** `Transaction` rows — one per
account — and each account's history needs to read correctly on its own
("money left" vs "money arrived").

---

## 4. Repositories — Spring Data JPA

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByCustomerId(Long customerId);
}
```

We write **zero implementation** here — this is one of Spring Data JPA's
core tricks. At startup, Spring generates a runtime proxy class that
implements this interface, based on two mechanisms:

1. **Extending `JpaRepository<Account, Long>`** — `Account` is the entity
   type, `Long` is its primary key type. This alone gives you `.save()`,
   `.findById()`, `.findAll()`, `.deleteById()`, `.count()`, etc. for free.
2. **Method name parsing** — Spring reads the method name word by word.
   `findByAccountNumber` is parsed as `find` + `By` + `AccountNumber`, and
   translated into `SELECT * FROM accounts WHERE account_number = ?`.
   `findByCustomerId` walks the object graph: `Account` → `customer` (the
   `@ManyToOne` field) → `id`, becoming
   `SELECT * FROM accounts WHERE customer_id = ?`.

`Optional<Account>` (rather than returning `Account` directly, possibly
`null`) forces every caller to explicitly handle the "not found" case —
see how `AccountServiceImpl` uses `.orElseThrow(...)` below — instead of
risking a `NullPointerException` several lines later.

`TransactionRepository` has one interesting method:

```java
List<Transaction> findByAccount_AccountNumberOrderByCreatedAtDesc(String accountNumber);
```

The underscore in `Account_AccountNumber` disambiguates the property path
— it means "the `accountNumber` field of the `account` (`@ManyToOne`)
relationship" — and `OrderByCreatedAtDesc` adds `ORDER BY created_at DESC`
to the generated query, so transaction history always comes back newest
first.

---

## 5. DTOs — why not just return entities?

Every controller talks in DTOs (`CustomerRequest`/`CustomerResponse`,
`AccountRequest`/`AccountResponse`, etc.), never raw `@Entity` objects.
Two concrete reasons:

1. **Different validation needs.** `CustomerRequest` has `@NotBlank`,
   `@Email` — rules about what a valid *incoming request* looks like. The
   `Customer` entity has no such annotations; its job is only to describe
   the *database schema*. Mixing the two would mean either the database
   schema gets polluted with API-specific concerns, or the API loses input
   validation.
2. **Preventing accidental data exposure and lazy-loading crashes.** If a
   controller returned a `Customer` entity directly, Jackson would try to
   serialize its `accounts` list (a `@OneToMany`, `LAZY` by default) —
   outside of an active Hibernate session, that throws a
   `LazyInitializationException`. Worse, any field later added to the
   entity (even one that should stay internal) would automatically leak
   into every API response with zero code change. A dedicated
   `CustomerResponse` class makes the public API contract an explicit,
   reviewable list of fields.

Example, `CustomerRequest.java`:

```java
public class CustomerRequest {
    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;
    ...
}
```

- `@NotBlank` rejects `null`, `""`, and whitespace-only strings.
- `@Email` checks the string matches a standard email pattern — but only
  runs *if* the value is non-null, which is why `@NotBlank` is still
  needed alongside it (otherwise a missing email would pass `@Email`'s
  check by doing nothing).
- These annotations do nothing on their own — they're inert metadata
  until a controller method parameter is annotated `@Valid` (see the
  Controllers section).

`ApiError.java` is the one DTO not tied to a specific domain concept — it's
the single, predictable JSON shape every error in this API returns
(timestamp, HTTP status, message, path, and an optional list of
field-level validation errors), so client code only needs one
error-parsing code path.

---

## 6. Exceptions and `GlobalExceptionHandler`

Three tiny domain exceptions:

```java
public class ResourceNotFoundException extends RuntimeException { ... }
public class DuplicateResourceException extends RuntimeException { ... }
public class InsufficientBalanceException extends RuntimeException { ... }
```

All extend `RuntimeException` (**unchecked**), not `Exception`
(**checked**). If they were checked, every method in the call chain —
service, controller — would be forced to either declare `throws
ResourceNotFoundException` or wrap every call in `try/catch`, which would
spread error-handling boilerplate throughout the codebase. As unchecked
exceptions, they can be thrown deep inside `TransactionServiceImpl` and
propagate all the way up through the controller, uncaught, until
`GlobalExceptionHandler` intercepts them.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    ...
}
```

- **`@RestControllerAdvice`** = `@ControllerAdvice` + `@ResponseBody`. It
  applies **globally**, to every `@RestController` in the app — so none of
  `CustomerController`, `AccountController`, or `TransactionController`
  needs its own try/catch blocks. Its methods' return values are
  serialized straight to JSON, exactly like a normal controller method.
- **`@ExceptionHandler(ResourceNotFoundException.class)`** — Spring
  inspects the runtime type of whatever exception escaped a controller
  call and routes it to whichever `@ExceptionHandler` method's declared
  type matches (or is a superclass of) it.
- Each domain exception maps to a specific, meaningful HTTP status:
  `ResourceNotFoundException` → `404`, `DuplicateResourceException` and
  `InsufficientBalanceException` → `409 Conflict` (the request was
  well-formed, but conflicts with the current state of the data).
- **`MethodArgumentNotValidException`** is thrown *by Spring itself*
  (not by our code) whenever a `@Valid`-annotated `@RequestBody` fails one
  or more Bean Validation constraints. Its handler flattens every field
  error into a `"field: message"` string list for the `validationErrors`
  field of `ApiError`.
- **`Exception.class`** (the catch-all at the bottom) guarantees that even
  a genuine bug (e.g. an unexpected `NullPointerException`) becomes a
  clean `500` JSON response instead of leaking a raw Java stack trace to
  API clients.

---

## 7. Services — where business logic lives

This is the most important layer to understand. Take
`TransactionServiceImpl.withdraw(...)` as the representative example:

```java
@Override
@Transactional
public TransactionResponse withdraw(WithdrawRequest request) {
    Account account = accountService.getAccountEntityByNumber(request.getAccountNumber());

    if (account.getBalance().compareTo(request.getAmount()) < 0) {
        throw new InsufficientBalanceException(...);
    }

    BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
    account.setBalance(newBalance);
    accountRepository.save(account);

    Transaction transaction = new Transaction(account, TransactionType.WITHDRAWAL,
            request.getAmount(), newBalance, request.getDescription(), null);
    Transaction saved = transactionRepository.save(transaction);

    return toResponse(saved);
}
```

- **`@Transactional`** wraps the *entire method body* in one database
  transaction. Spring opens it on entry and either commits every change
  (if the method returns normally) or rolls back **all** of them (if any
  `RuntimeException` escapes). This one line is why a withdrawal can never
  end up in the broken state "balance was debited, but no ledger row was
  written" (or vice versa) — both the `Account` update and the
  `Transaction` insert succeed together, or neither does. This matters
  even more in `transfer(...)`, which touches **two** accounts and writes
  **two** ledger rows — see that method's comments in the source for the
  full reasoning.
- **`account.getBalance().compareTo(request.getAmount()) < 0`** — the
  correct way to compare two `BigDecimal` values. `<` doesn't even compile
  on `BigDecimal` (it's an object, not a primitive), and using `.equals()`
  elsewhere would be a subtle bug: `new BigDecimal("10.0").equals(new
  BigDecimal("10.00"))` is `false` (different `scale`), even though they
  represent the same amount of money. `.compareTo()` correctly treats them
  as equal.
- **Constructor injection** (seen in every service's constructor, e.g.
  `CustomerServiceImpl(CustomerRepository customerRepository)`) — Spring
  sees this is the class's only constructor and automatically supplies a
  matching bean when it creates the service. This is preferred over
  `@Autowired` on a field for two reasons: the field can be declared
  `final` (truly immutable once constructed), and tests can call `new
  CustomerServiceImpl(mockRepository)` directly with zero Spring container
  involved (see `CustomerServiceImplTest`).
- **Interface + `impl` package split** (`CustomerService` vs
  `CustomerServiceImpl`) — controllers depend only on the interface. This
  means a test can substitute a totally different implementation (a mock)
  without the controller code knowing or caring, which is exactly how
  `CustomerControllerTest` works via `@MockBean`.
- **Manual entity → DTO mapping** (the private `toResponse(...)` method at
  the bottom of each service) is written out explicitly, field by field,
  rather than using a mapping library like MapStruct. For a learning
  project, this trades a small amount of boilerplate for total
  transparency — you can see exactly where every response field comes
  from.

`AccountServiceImpl.openAccount(...)` has one more detail worth calling
out: it writes an **opening `Transaction` row** (type `DEPOSIT`) whenever
`initialDeposit > 0`, in the same `@Transactional` method as creating the
`Account`. This wasn't optional — every event that changes a balance,
including the very first one, has to leave a ledger trace, or an account's
history wouldn't add up to its current balance.

`AccountService` also exposes one method controllers **never** call:

```java
Account getAccountEntityByNumber(String accountNumber);
```

This returns the managed JPA **entity**, not a DTO, specifically so
`TransactionServiceImpl` can mutate its `balance` field and persist that
change inside its own `@Transactional` method. A DTO is just data — it has
no connection back to a database row, so it can't be used for an update
like this.

---

## 8. Controllers — the HTTP layer

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }
}
```

- **`@RestController`** = `@Controller` + `@ResponseBody`: Spring routes
  matching HTTP requests here, and every method's return value is
  serialized directly to the HTTP response body as JSON (via Jackson)
  rather than being treated as an HTML view name.
- **`@RequestMapping("/api/customers")`** at the class level sets the
  common URL prefix; each method only declares its suffix.
- **`@PostMapping`** / **`@GetMapping("/{id}")`** — shorthand for
  `@RequestMapping(method = POST)` / `GET`, combined with the class-level
  prefix, so `createCustomer` handles `POST /api/customers` and
  `getCustomer` handles `GET /api/customers/{id}`.
- **`@RequestBody CustomerRequest request`** — deserializes the incoming
  JSON request body into a `CustomerRequest` Java object.
- **`@Valid`** — placed *before* `@RequestBody`, this tells Spring to run
  every Bean Validation constraint on the deserialized object **before**
  the method body executes. If any fail, Spring throws
  `MethodArgumentNotValidException`, which — as covered above —
  `GlobalExceptionHandler` turns into a clean `400` response. The
  controller method body never even runs in that case.
- **`@PathVariable Long id`** — binds the `{id}` URL segment to this
  parameter, with Spring automatically converting the URL's string
  (`"5"`) into a `Long`.
- **`ResponseEntity<T>`** — lets us control the HTTP status code
  explicitly. `new ResponseEntity<>(response, HttpStatus.CREATED)` returns
  `201 Created` (the correct status for "a new resource now exists"),
  while `ResponseEntity.ok(...)` is shorthand for `200 OK`.
- **Controllers contain no business logic.** Every method here is a thin
  translation layer: pull data out of the HTTP request, hand it to the
  service, wrap the service's return value in the right HTTP status. All
  three controllers (`Customer`, `Account`, `Transaction`) follow this
  exact shape — once you understand one, you understand all of them.

`@Tag` and `@Operation` (from `springdoc-openapi`) are purely cosmetic —
they only affect how endpoints are labeled and described in the generated
Swagger UI page; removing them would not change the API's behavior at
all.

---

## 9. Configuration files

**`OpenApiConfig.java`**:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI().info(new Info().title("Banking Application API")...);
    }
}
```

- **`@Configuration`** marks this class as a source of manually-defined
  beans (as opposed to `@Component`/`@Service` auto-detected by
  `@ComponentScan`).
- **`@Bean`** — this method runs once at startup, and its return value
  (an `OpenAPI` object) is registered into the Spring container.
  `springdoc-openapi` automatically finds and uses it to customize the
  title/description shown at the top of the Swagger UI page.

**`application.yml`** — see the inline comments in the file itself for a
line-by-line breakdown of every setting; the short version:
- `spring.profiles.active: h2` selects which database profile is active
  by default.
- `spring.jpa.hibernate.ddl-auto: update` tells Hibernate to create/alter
  tables automatically to match the `@Entity` classes — convenient for
  learning, but something a real production system would replace with a
  proper migration tool (see the README's "production considerations"
  section).
- `spring.h2.console.enabled: true` turns on the browser-based database
  viewer at `/h2-console`.

**`application-mysql.yml`** / **`application-postgres.yml`** — profile
files that *override* specific keys from `application.yml` (mainly
`spring.datasource.*`) when that profile is active. Anything not repeated
in these files (like `server.port`) still comes from the base
`application.yml`. See `docs/DATABASE_SETUP.md` for how to actually
activate one.
