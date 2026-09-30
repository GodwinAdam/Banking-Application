# API Testing Guide

Three ways to test every endpoint below, in increasing order of
convenience: automated tests (already in the repo — see the README), raw
`curl` commands, and Swagger UI / Postman for interactive testing. All the
`curl` examples on this page were actually run against the live app while
this project was built — the shown responses are real, not hypothetical.

Start the app first:

```bash
mvn spring-boot:run
```

It listens on **http://localhost:8080**.

## Option 1: Swagger UI (easiest, no tools needed)

1. Open http://localhost:8080/swagger-ui.html
2. You'll see three groups — **Customers**, **Accounts**, **Transactions**
   — matching the three controllers.
3. Click any endpoint → **Try it out** → edit the example JSON body →
   **Execute**. The response, headers, and status code appear right below.

This is the fastest way to explore the API without installing anything.

## Option 2: Postman

1. Import via **File → New → HTTP Request**, or create a new collection
   named "Banking Application".
2. Set the base URL to `http://localhost:8080`.
3. For each endpoint below, create a request with the matching method,
   path, and (for POST requests) a JSON body with header
   `Content-Type: application/json`.
4. Alternatively: open http://localhost:8080/v3/api-docs, save that JSON,
   and use Postman's **Import → OpenAPI** to generate every request
   automatically from the live spec.

## Option 3: curl (scriptable, shown below for every endpoint)

### 1. Create a customer

```bash
curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Anderson",
    "email": "alice@example.com",
    "phoneNumber": "9876543210",
    "address": "221B Baker Street"
  }'
```

Response (`201 Created`):

```json
{"id":1,"fullName":"Alice Anderson","email":"alice@example.com","phoneNumber":"9876543210","address":"221B Baker Street","createdAt":"2026-09-27T21:17:00.130568"}
```

### 2. Get a customer by id

```bash
curl -s http://localhost:8080/api/customers/1
```

### 3. List all customers

```bash
curl -s http://localhost:8080/api/customers
```

### 4. Open an account (with an initial deposit)

```bash
curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountType": "SAVINGS",
    "initialDeposit": 1000.00
  }'
```

Response (`201 Created`):

```json
{"id":1,"accountNumber":"AC28654559","accountType":"SAVINGS","balance":1000.00,"customerId":1,"customerName":"Alice Anderson","createdAt":"2026-09-27T21:17:01.080838"}
```

`accountType` must be `SAVINGS` or `CURRENT` (case-sensitive — it's a Java
enum). Copy the returned `accountNumber` — every transaction endpoint below
uses it.

### 5. Get account details / balance

```bash
curl -s http://localhost:8080/api/accounts/AC28654559
```

### 6. List a customer's accounts

```bash
curl -s http://localhost:8080/api/accounts/customer/1
```

### 7. Deposit

```bash
curl -s -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "AC28654559",
    "amount": 200.00,
    "description": "Bonus"
  }'
```

Response (`200 OK`):

```json
{"id":2,"accountNumber":"AC28654559","transactionType":"DEPOSIT","amount":200.00,"balanceAfterTransaction":1200.00,"description":"Bonus","relatedAccountNumber":null,"createdAt":"2026-09-27T21:17:01.143279"}
```

### 8. Withdraw

```bash
curl -s -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "AC28654559",
    "amount": 300.00
  }'
```

Response (`200 OK`):

```json
{"id":3,"accountNumber":"AC28654559","transactionType":"WITHDRAWAL","amount":300.00,"balanceAfterTransaction":900.00,"description":null,"relatedAccountNumber":null,"createdAt":"2026-09-27T21:17:01.169894"}
```

### 9. Transfer between two accounts

Open a second account first (repeat step 4 with a different `customerId`
or the same one), then:

```bash
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "AC28654559",
    "toAccountNumber": "AC10023456",
    "amount": 400.00,
    "description": "Rent"
  }'
```

The response is the **sender's** side of the transfer
(`transactionType: "TRANSFER_OUT"`); the recipient's account gets a
matching `TRANSFER_IN` row, visible in their own history (endpoint 10).

### 10. Transaction history (newest first)

```bash
curl -s http://localhost:8080/api/transactions/AC28654559
```

Response after the deposit + withdraw above (`200 OK`) — note the opening
balance itself is also a ledger entry:

```json
[
  {"id":3,"accountNumber":"AC28654559","transactionType":"WITHDRAWAL","amount":300.00,"balanceAfterTransaction":900.00,"description":null,"relatedAccountNumber":null,"createdAt":"2026-09-27T21:17:01.169894"},
  {"id":2,"accountNumber":"AC28654559","transactionType":"DEPOSIT","amount":200.00,"balanceAfterTransaction":1200.00,"description":"Bonus","relatedAccountNumber":null,"createdAt":"2026-09-27T21:17:01.143279"},
  {"id":1,"accountNumber":"AC28654559","transactionType":"DEPOSIT","amount":1000.00,"balanceAfterTransaction":1000.00,"description":"Initial deposit on account opening","relatedAccountNumber":null,"createdAt":"2026-09-27T21:17:01.082204"}
]
```

## Testing error cases

A well-tested API isn't just the happy path — try these too:

### Insufficient balance → `409 Conflict`

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{"accountNumber": "AC28654559", "amount": 999999.00}'
```

```json
{"timestamp":"2026-09-27T21:17:01.232002","status":409,"error":"Conflict","message":"Insufficient balance in account AC28654559. Available: 900.00, requested: 999999.00","path":"/api/transactions/withdraw","validationErrors":null}
HTTP_STATUS:409
```

### Validation failure → `400 Bad Request`

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"fullName": "", "email": "not-an-email", "phoneNumber": ""}'
```

```json
{"timestamp":"2026-09-27T21:17:01.252954","status":400,"error":"Bad Request","message":"Validation failed","path":"/api/customers","validationErrors":["phoneNumber: phoneNumber is required","email: email must be a valid email address","fullName: fullName is required"]}
HTTP_STATUS:400
```

### Not found → `404 Not Found`

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8080/api/customers/999999
```

### Duplicate email → `409 Conflict`

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Someone Else", "email": "alice@example.com", "phoneNumber": "1112223333"}'
```

(Assuming `alice@example.com` was already used in step 1 above.)

## Running the automated tests instead

All of the flows above are also captured as automated tests you can run
with one command — see the README's [Testing](../README.md#testing)
section:

```bash
mvn test
```

In particular, `BankingApplicationIntegrationTest.java` runs almost this
exact sequence (create customers, open accounts, deposit, withdraw,
transfer, check history, and every error case) against a real running
instance of the app automatically, and asserts on every response — useful
as a reference for what "correct" looks like for each call.
