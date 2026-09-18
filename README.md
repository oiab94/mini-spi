# Mini SPI

Mini SPI es un proyecto Spring Boot 4 para transferencias inmediatas (SPI) con PostgreSQL y soporte Docker.

## Requisitos

- Java 21
- Docker y Docker Compose
- Git (opcional, para control de cambios)

## Iniciar con Docker

1. Ubica el proyecto en la carpeta donde lo hayas clonado o guardado.
2. Desde la raíz del proyecto, entra a esa ruta y levanta los servicios en este orden recomendado:

```bash
cd /ruta/donde/guardaste/mini-spi
```

### Levantar primero la base de datos

```bash
docker compose up -d db
```

Espera a que la base de datos esté healthy antes de arrancar la API:

```bash
docker compose ps
```

Cuando la columna `State` de `db` muestre `healthy`, entonces levanta la API:

```bash
docker build -t mini-spi-api .
docker compose up -d api
```

Ejemplo completo:

```bash
cd /home/oscar/Documentos/proyectos/mini-spi
docker compose up -d db
docker compose ps
docker build -t mini-spi-api .
docker compose up -d api
```

### Verificar que los servicios estén activos

```bash
docker compose ps
```

La API queda disponible en:

```text
http://localhost:8080/api/v1/transferencias
```

La base de datos queda disponible en:

```text
localhost:5432
usuario: postgres
password: postgres
database: mini_spi
```

> Si el puerto 8080 ya está ocupado en tu máquina, cambia el mapeo en `docker-compose.yml` a `8081:8080` y prueba con `http://localhost:8081/api/v1/transferencias`.

## Limpiar entorno Docker

Cuando hayas terminado de probar, puedes resetear el entorno para una entrega limpia. Primero accede al proyecto y ejecuta:

```bash
cd /ruta/donde/guardaste/mini-spi
docker compose down -v --remove-orphans || true
docker ps -aq | xargs -r docker rm -f || true
docker volume ls -q | xargs -r docker volume rm || true
docker rmi mini-spi-api 2>/dev/null || true
```

Ejemplo:

```bash
cd /home/oscar/Documentos/proyectos/mini-spi
docker compose down -v --remove-orphans || true
docker ps -aq | xargs -r docker rm -f || true
docker volume ls -q | xargs -r docker volume rm || true
docker rmi mini-spi-api 2>/dev/null || true
```

## Endpoint principal

```http
POST /api/v1/transferencias
```

Headers requeridos:

```http
Content-Type: application/json
X-Idempotency-Key: <clave-unica>
```

## Casos de prueba con curl

### 1) Transferencia exitosa

```bash
curl -i -X POST http://localhost:8080/api/v1/transferencias \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: key-success-001" \
  -d '{
    "sourceAccount": {
      "number": "ACC-000001",
      "name": "Alice"
    },
    "targetAccount": {
      "number": "ACC-000002",
      "name": "Bob"
    },
    "amount": 250.00,
    "status": "PENDIENTE",
    "createdTimestamp": "2026-09-18T16:00:00Z"
  }'
```

> La ruta del proyecto debe ser la que hayas usado al hacer `cd /ruta/donde/guardaste/mini-spi` antes de ejecutar estas pruebas.

### 2) Transferencia rechazada por fondos insuficientes

```bash
curl -i -X POST http://localhost:8080/api/v1/transferencias \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: key-rejected-001" \
  -d '{
    "sourceAccount": {
      "number": "ACC-000001",
      "name": "Alice"
    },
    "targetAccount": {
      "number": "ACC-000002",
      "name": "Bob"
    },
    "amount": 999999.00,
    "status": "PENDIENTE",
    "createdTimestamp": "2026-09-18T16:00:00Z"
  }'
```

### 3) Validación de idempotencia

```bash
curl -i -X POST http://localhost:8080/api/v1/transferencias \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: key-success-001" \
  -d '{
    "sourceAccount": {
      "number": "ACC-000001",
      "name": "Alice"
    },
    "targetAccount": {
      "number": "ACC-000002",
      "name": "Bob"
    },
    "amount": 250.00,
    "status": "PENDIENTE",
    "createdTimestamp": "2026-09-18T16:00:00Z"
  }'
```

Este tercer curl debe devolver el mismo estado vigente de la primera transferencia, en lugar de volver a procesarla.

## Respuestas esperadas

### Transferencia exitosa

```json
{
  "idempotencyKey": "key-success-001",
  "status": "EXITOSA",
  "message": "Transfer completed successfully.",
  "sourceAccountNumber": "ACC-000001",
  "targetAccountNumber": "ACC-000002",
  "amount": 250.0,
  "createdTimestamp": "2026-09-18T16:00:00Z"
}
```

### Transferencia rechazada por saldo

```json
{
  "idempotencyKey": "key-rejected-001",
  "status": "RECHAZADA",
  "message": "Insufficient funds. The transfer was rejected.",
  "sourceAccountNumber": "ACC-000001",
  "targetAccountNumber": "ACC-000002",
  "amount": 999999.0,
  "createdTimestamp": "2026-09-18T16:00:00Z"
}
```

### Idempotencia

```json
{
  "idempotencyKey": "key-success-001",
  "status": "EXITOSA",
  "message": "The transfer already exists with the current idempotency key.",
  "sourceAccountNumber": "ACC-000001",
  "targetAccountNumber": "ACC-000002",
  "amount": 250.0,
  "createdTimestamp": "2026-09-18T16:00:00Z"
}
```

## Reglas de negocio implementadas

- Idempotencia por `X-Idempotency-Key`
- Validación de cuentas origen y destino
- Validación de saldo suficiente
- Registro transaccional de transferencias
- Simulación de red bancaria con reintentos
- Estado final `PENDIENTE`, `EXITOSA` o `RECHAZADA`
- Base de datos PostgreSQL con datos semilla de cuentas

## Notas

- La API usa Java 21.
- Los tests usan H2 para validación rápida.
- La batería de datos inicial incluye 100 cuentas precargadas.
- El proyecto queda listo para levantarse con Docker sin necesidad de instalar PostgreSQL localmente.
