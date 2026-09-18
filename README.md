# Mini SPI

Mini SPI is a Spring Boot 4 project for immediate transfers (SPI) with PostgreSQL persistence and Docker support.

## Requirements

- Java 21
- Maven Wrapper included in the project
- Docker and Docker Compose

## Quick start with Docker

1. Build and run the stack:

```bash
cd /home/oscar/Documentos/proyectos/mini-spi
docker compose up --build
```

2. API endpoint available at:

```text
http://localhost:8080/api/v1/transferencias
```

3. PostgreSQL is available at:

```text
localhost:5432
user: postgres
password: postgres
database: mini_spi
```

## Local run without Docker

1. Start a PostgreSQL instance locally or use the Docker database.
2. Run the app:

```bash
cd /home/oscar/Documentos/proyectos/mini-spi
export JAVA_HOME=/home/oscar/.jdks/ms-21.0.12.1
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/mini_spi --spring.datasource.username=postgres --spring.datasource.password=postgres"
```

## Transfer endpoint

### Endpoint

```http
POST /api/v1/transferencias
```

### Headers

```http
Content-Type: application/json
X-Idempotency-Key: <unique-key>
```

### Example request

```json
{
  "sourceAccount": {
    "number": "ACC-000001",
    "name": "Alice"
  },
  "targetAccount": {
    "number": "ACC-000002",
    "name": "Bob"
  },
  "amount": 500.00,
  "status": "PENDIENTE",
  "createdTimestamp": "2026-09-18T16:00:00Z"
}
```

### Example response

```json
{
  "idempotencyKey": "key-001",
  "status": "EXITOSA",
  "message": "Transfer completed successfully.",
  "sourceAccountNumber": "ACC-000001",
  "targetAccountNumber": "ACC-000002",
  "amount": 500.0,
  "createdTimestamp": "2026-09-18T19:39:35.322235118Z"
}
```

## Business rules implemented

- Idempotency by `X-Idempotency-Key`
- Duplicate key returns the current transfer state
- Validation of source and target accounts
- Sufficient funds check
- Transactional update for account balances
- Simulated external bank retry logic
- Rejected transfers stored with `RECHAZADA` state
- Successful transfers stored with `EXITOSA` state

## Notes

- The project uses Java 21.
- Runtime database is PostgreSQL.
- Test profile uses H2.
- The `db/init.sql` script seeds 100 accounts to simulate a real banking dataset.
