# AGENTS.md

## Project purpose
This repository contains the initial Spring Boot application for a banking module that processes immediate transfers (SPI). The goal is to implement a secure and reliable transfer service that validates funds, handles idempotency, simulates external bank network failures, and guarantees transactional consistency.

## Technology stack
- Java 21+
- Spring Boot 4.x
- PostgreSQL
- Docker and Docker Compose
- Maven wrapper

## Core rules for all future changes

### Code style
- All production code must be written in English.
- All variable, method, class, package, and JSON field names must use English names.
- Function comments must be written in Spanish and describe the purpose of the service in a clear, practical way.
- Keep the code clean, explicit, and consistent with Spring Boot conventions.

### API style
- All endpoints must be under the `/api/v1` prefix.
- Use RESTful patterns and standard Spring MVC controllers.
- The current endpoint to implement is:
  - `POST /api/v1/transferencias`
- Required header:
  - `X-Idempotency-Key`

### Request contract
Request body (JSON):

```json
[
  {
    "sourceAccount": {
      "number": "123456789",
      "name": "Alice Johnson"
    },
    "targetAccount": {
      "number": "987654321",
      "name": "Bob Smith"
    },
    "amount": 250.00,
    "status": "PENDIENTE",
    "createdTimestamp": "2026-09-18T12:00:00Z"
  }
]
```

Notes:
- `sourceAccount.number` and `targetAccount.number` must be strings.
- `amount` must be a valid monetary amount.
- `status` defaults to `PENDIENTE` when it is not explicitly provided.
- `createdTimestamp` should be captured in UTC.

## Business requirements

### Idempotency
- Every transfer request must check whether the same idempotency key was already processed.
- If the transfer already exists, the system must return the existing status instead of re-processing it.
- `idempotency_key` must be unique in the database.

### Fund validation
- The system must validate that the source account has enough balance before debiting.
- If the account does not have sufficient funds, the transaction must fail with the corresponding state and an actionable message.

### Transaction safety
- Every transfer must be transactional.
- If any part of the flow fails, the transaction must be rolled back and the source account balance must be restored.
- All transfer processing must preserve consistency between account balances and transfer records.

### Fault simulation
- Simulate external bank network failures randomly in at least 20% of incoming transactions.
- The simulation must be deterministic enough for testing but random enough to validate retry behavior.

### Retry pattern
- Retry external bank communication up to 3 attempts.
- If all retries fail, mark the transfer as `RECHAZADA` and perform the refund process.
- No partial debit should remain after a failed external communication workflow.

### Status values
- `PENDIENTE`
- `RECHAZADA`
- `EXITOSA`

## Database design
Use PostgreSQL and create the initial data set through an `init.sql` script. At minimum, the script should populate the database with 100 random users and corresponding account records.

### Tables
#### CUENTAS
- `id`
- `account_number` (unique)
- `saldo`
- `createdTimestamp`
- `updatedTimestamp`

#### TRANSFERENCIAS
- `id`
- `account_source_number`
- `account_target_number`
- `idempotency_key` (unique)
- `status`
- `createdTimestamp`
- `updatedTimestamp`

### Stored procedure requirement
The application must process incoming transfer payloads through a database stored procedure. The stored procedure should coordinate validation, state updates, and the transfer lifecycle with the database as the source of truth. The implementation must be built around controlled transactional behavior and idempotency enforcement.

## Docker and runtime
- Provide a Dockerfile for the application image.
- Provide a Docker Compose file that starts both the API and the PostgreSQL database.
- Keep the service configuration easy to bootstrap locally for validation.

## Expected implementation structure
Use a Spring Boot structure consistent with a clean service-oriented layout, such as:

- `controller/`
- `service/`
- `repository/`
- `model/`
- `dto/`
- `exception/`
- `config/`
- `database/` or `scripts/`

## Git and delivery rules
- All code changes must be reviewed before acceptance.
- Commit messages should be written in English.
- Preserve a clear change history and update the project tracking notes in `.copilot/PLAN.md` as the work progresses.
- Keep the implementation incremental and verifiable.

## Acceptance criteria for this module
- The endpoint processes valid transfers and records them correctly.
- Duplicate requests with the same idempotency key do not create duplicate transfers.
- Insufficient funds are rejected safely.
- External failures trigger retries and, on final failure, a refund process.
- Transactions are atomic across the database and the business flow.
- PostgreSQL is initialized with realistic sample data.
- Docker Compose can run the API and database locally.
