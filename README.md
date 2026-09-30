# Banking Application — Spring Boot Learning Project

A small but realistic **Java 25 + Spring Boot 3 + Spring Data JPA** REST API
for a bank: customers, accounts, and a transaction ledger (deposit,
withdraw, transfer, history). Built specifically to **teach** the standard
layered structure used in real Spring Boot projects — every file has
comments explaining *why* each line exists, not just what it does, and
`docs/CODE_WALKTHROUGH.md` walks through every class in more depth.

## Table of contents

- [Tech stack](#tech-stack)
- [Folder structure](#folder-structure)
- [Architecture: how a request flows through the app](#architecture-how-a-request-flows-through-the-app)
- [Prerequisites](#prerequisites)
- [Running the app](#running-the-app)
- [Database](#database)
- [API endpoints](#api-endpoints)
- [Testing](#testing)
- [Documentation index](#documentation-index)
- [What's deliberately left out](#whats-deliberately-left-out-production-considerations)

## Tech stack

| Concern              | Choice                                   | Why |
|-----------------------|-------------------------------------------|-----|
| Language              | Java 25                                   | Current LTS release |
| Framework             | Spring Boot 3.5.16                        | Industry-standard Java web framework |
| Web layer             | Spring MVC (`spring-boot-starter-web`)    | REST controllers over embedded Tomcat |
| Persistence           | Spring Data JPA + Hibernate                | No hand-written SQL for basic CRUD |
| Local database        | H2 (in-memory)                             | Zero setup — clone and run |
| Real databases        | MySQL / PostgreSQL (drivers included)      | See [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md) |
| Validation            | Jakarta Bean Validation (`@NotBlank`, etc.)| Rejects bad input before it reaches business logic |
| API docs              | springdoc-openapi (Swagger UI)             | Interactive, browser-based API testing |
| Build tool            | Maven                                      | `pom.xml` declares every dependency version |
| Testing               | JUnit 5, Mockito, MockMvc, AssertJ        | Unit, slice, and full end-to-end tests |

## Folder structure

```
banking-application/
├── pom.xml                          # Maven build file — every dependency & plugin
├── README.md                        # You are here
├── docs/
│   ├── CODE_WALKTHROUGH.md          # Line-by-line explanation of every class
│   ├── DATABASE_SETUP.md            # How to switch from H2 to MySQL/PostgreSQL
│   └── API_TESTING.md               # curl / Postman / Swagger examples for every endpoint
├── src/main/java/com/bankingapp/
│   ├── BankingApplication.java      # main() — boots the Spring container
│   ├── config/
│   │   └── OpenApiConfig.java       # Swagger UI title/description customization
│   ├── controller/                  # @RestController classes — HTTP in, JSON out
│   │   ├── CustomerController.java
│   │   ├── AccountController.java
│   │   └── TransactionController.java
│   ├── dto/                         # Request/response JSON shapes (never expose entities directly)
│   │   ├── CustomerRequest.java / CustomerResponse.java
│   │   ├── AccountRequest.java  / AccountResponse.java
│   │   ├── DepositRequest.java, WithdrawRequest.java, TransferRequest.java
│   │   ├── TransactionResponse.java
│   │   └── ApiError.java            # One consistent JSON error shape for the whole API
│   ├── entity/                      # @Entity classes — one per database table
│   │   ├── Customer.java
│   │   ├── Account.java
│   │   ├── Transaction.java
│   │   └── enums/
│   │       ├── AccountType.java     # SAVINGS, CURRENT
│   │       └── TransactionType.java # DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN
│   ├── repository/                  # Spring Data JPA interfaces — zero SQL written by us
│   │   ├── CustomerRepository.java
│   │   ├── AccountRepository.java
│   │   └── TransactionRepository.java
│   ├── service/                     # Interfaces = the contract controllers depend on
│   │   ├── CustomerService.java, AccountService.java, TransactionService.java
│   │   └── impl/                    # Business logic implementations
│   │       ├── CustomerServiceImpl.java
│   │       ├── AccountServiceImpl.java
│   │       └── TransactionServiceImpl.java   # Deposit/withdraw/transfer logic, @Transactional
│   ├── exception/
│   │   ├── ResourceNotFoundException.java, DuplicateResourceException.java,
│   │   │   InsufficientBalanceException.java
│   │   └── GlobalExceptionHandler.java       # @RestControllerAdvice — one place for all error mapping
│   └── util/
│       └── AccountNumberGenerator.java       # Generates "AC12345678"-style account numbers
├── src/main/resources/
│   ├── application.yml               # Default config: H2, JPA, Swagger, logging
│   ├── application-mysql.yml         # Activate with -Dspring.profiles.active=mysql
│   └── application-postgres.yml      # Activate with -Dspring.profiles.active=postgres
└── src/test/java/com/bankingapp/
    ├── service/                       # Unit tests (Mockito, no Spring context, no DB)
    │   ├── CustomerServiceImplTest.java
    │   └── TransactionServiceImplTest.java
    ├── controller/                    # Slice tests (@WebMvcTest, real Jackson+validation, mocked service)
    │   ├── CustomerControllerTest.java
    │   └── TransactionControllerTest.java
    └── BankingApplicationIntegrationTest.java  # Full end-to-end (@SpringBootTest, real H2, real HTTP)
```

This is the **standard layered architecture** used across almost all real
Spring Boot backends, regardless of domain:

```
controller  →  service (interface)  →  service.impl  →  repository  →  database
   ↑                                                          ↓
  dto  ←───────────────── entity ←─────────────────────────────
```

- **`controller`** only knows about HTTP (paths, status codes, request/response bodies). It never touches the database directly.
- **`service`** holds all business rules ("can this withdrawal happen?"). Controllers depend on the *interface*, never the `impl` class, which is what makes mocking trivial in tests.
- **`repository`** is pure data access — Spring Data JPA generates the implementation for us.
- **`entity`** classes are the database schema, expressed as Java objects. They are never returned directly from a controller.
- **`dto`** classes are the public API contract — separate from `entity` on purpose, so the database schema can evolve independently of what clients see.

## Architecture: how a request flows through the app

Example: `POST /api/transactions/deposit`

1. **Tomcat** (embedded in Spring Boot) receives the raw HTTP request.
2. **Spring MVC** matches the URL + method to `TransactionController.deposit(...)`.
3. Jackson deserializes the JSON body into a `DepositRequest` object.
4. `@Valid` runs Bean Validation (`@NotBlank`, `@DecimalMin`, ...) on that object. If it fails, `GlobalExceptionHandler` returns a `400` immediately — the controller method body never runs.
5. The controller calls `transactionService.deposit(request)` — it has no idea *how* that's implemented, only that it returns a `TransactionResponse`.
6. `TransactionServiceImpl`, inside a single `@Transactional` block:
   - loads the `Account` entity via `AccountService`,
   - checks/updates its balance,
   - saves the updated `Account` and a new `Transaction` row via their repositories.
7. If anything throws inside step 6, the whole database transaction rolls back — no half-applied state.
8. The service maps the saved `Transaction` entity to a `TransactionResponse` DTO and returns it.
9. The controller wraps it in a `ResponseEntity` (HTTP 200) and Jackson serializes it back to JSON.

## Prerequisites

- Java 25+ (`java -version`)
- Maven 3.9+ (`mvn -version`) — or use your IDE's built-in Maven support
- No database installation needed to run locally (H2 is embedded)

## Running the app

```bash
cd banking-application
mvn spring-boot:run
```

The app starts on **http://localhost:8080**. You should see Spring Boot's
banner in the console, then a log line like:
`Tomcat started on port 8080`.

Useful URLs once it's running:

| URL                                   | What it is |
|----------------------------------------|------------|
| http://localhost:8080/swagger-ui.html  | Interactive API docs — call every endpoint from the browser |
| http://localhost:8080/h2-console       | H2's web-based database browser (see below for login) |
| http://localhost:8080/v3/api-docs      | Raw OpenAPI JSON spec |

**H2 console login:**
- JDBC URL: `jdbc:h2:mem:bankingdb`
- User: `sa`
- Password: *(leave blank)*

To build an executable jar instead:

```bash
mvn clean package
java -jar target/banking-application-1.0.0.jar
```

## Database

By default the app uses an **in-memory H2 database** — perfect for
learning, since there's nothing to install and every restart gives you a
clean slate. `spring.jpa.hibernate.ddl-auto=update` means Hibernate creates
the `customers`, `accounts`, and `transactions` tables automatically from
the `@Entity` classes at startup.

To connect this same code to a **real, persistent MySQL or PostgreSQL
database** (both drivers are already in `pom.xml`), see the full
step-by-step guide: **[docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md)**.
It covers installing the database (including a Docker one-liner), creating
the schema, and which one line to change to switch profiles.

## API endpoints

| Method | Path                                   | Purpose |
|--------|------------------------------------------|---------|
| POST   | `/api/customers`                          | Create a customer |
| GET    | `/api/customers/{id}`                     | Get one customer |
| GET    | `/api/customers`                          | List all customers |
| POST   | `/api/accounts`                           | Open an account for a customer |
| GET    | `/api/accounts/{accountNumber}`           | Get account details + balance |
| GET    | `/api/accounts/customer/{customerId}`     | List a customer's accounts |
| POST   | `/api/transactions/deposit`               | Deposit money |
| POST   | `/api/transactions/withdraw`              | Withdraw money |
| POST   | `/api/transactions/transfer`              | Transfer between two accounts |
| GET    | `/api/transactions/{accountNumber}`       | Full transaction history (newest first) |

Full request/response JSON examples and ready-to-paste `curl` commands for
every one of these are in **[docs/API_TESTING.md](docs/API_TESTING.md)**.

## Testing

This project shows all three common levels of Spring Boot testing:

| Test type       | Example file                                | What it starts | Speed |
|-----------------|----------------------------------------------|-----------------|-------|
| Unit test       | `service/TransactionServiceImplTest.java`     | Nothing — Mockito fakes the repositories | Milliseconds |
| Slice test      | `controller/CustomerControllerTest.java`      | Just the web layer (`@WebMvcTest`), service mocked | Fast |
| Integration test| `BankingApplicationIntegrationTest.java`      | The entire app + real embedded Tomcat + real H2 DB | Slower, most realistic |

Run everything:

```bash
mvn test
```

Run a single test class:

```bash
mvn test -Dtest=TransactionServiceImplTest
```

See **[docs/API_TESTING.md](docs/API_TESTING.md)** for how to manually test
each endpoint with `curl`, Postman, or Swagger UI once the app is running.

## Documentation index

| Doc | Contents |
|-----|----------|
| [docs/CODE_WALKTHROUGH.md](docs/CODE_WALKTHROUGH.md) | Every significant file, explained section by section: what each annotation does and why it's needed |
| [docs/DATABASE_SETUP.md](docs/DATABASE_SETUP.md) | Switching from H2 to a real local/remote MySQL or PostgreSQL database, with Docker commands |
| [docs/API_TESTING.md](docs/API_TESTING.md) | curl commands, Postman instructions, and Swagger UI walkthrough for every endpoint, including error cases |

## What's deliberately left out (production considerations)

This is a *learning* project, scoped to teach structure and JPA/REST
fundamentals clearly. A production banking system would additionally need:

- **Authentication/authorization** (Spring Security + OAuth2/JWT) — this API is currently open.
- **Schema migrations** (Flyway/Liquibase) instead of `ddl-auto: update`, so schema changes are versioned and reviewable.
- **Optimistic or pessimistic locking** on `Account.balance` (e.g. a JPA `@Version` field) to guard against two concurrent transfers on the same account racing each other.
- **Idempotency keys** on money-movement endpoints, so a retried network request can't double-charge an account.
- **Structured audit logging** and monitoring/alerting.
- **Pagination** on the "list all customers" / "transaction history" endpoints, which currently return every row.

These are called out here rather than added silently, since introducing
them changes what there is to learn from the code as written.
