# Plan de trabajo del proyecto mini-spi

## Objetivo
Diseñar y documentar las reglas operativas del módulo SPI para transferencias inmediatas antes de la implementación funcional del backend.

## Problema
El proyecto inicia con una base Spring Boot vacía, pero tiene un alcance claro de negocio: transferencias bancarias con idempotencia, validación de saldo, manejo de errores externos, transacciones en base de datos y despliegue con Docker.

## Enfoque
Se debe preparar un AGENTS.md en la raíz del proyecto con instrucciones para el trabajo futuro y un registro de planificación dentro de `.copilot/PLAN.md` para mantener trazabilidad del proyecto.

## Trabajo pendiente
1. Definir la estructura general y convenciones del proyecto.
2. Documentar API REST y contrato de transferencia.
3. Definir reglas de negocio, estado de transacciones y manejo de retry.
4. Especificar diseño de base de datos PostgreSQL e init.sql.
5. Documentar despliegue con Docker y Docker Compose.
6. Preparar la hoja de ruta para la implementación real de servicios, repositorios y pruebas.

## Decisiones relevantes
- El código debe estar en inglés, con comentarios funcionales en español.
- Las transferencias deben ser idempotentes con `X-Idempotency-Key`.
- La lógica de fondos debe validar saldo antes del débito.
- Las fallas externas se simulan aleatoriamente y se gestionan con retries.
- Las operaciones deben ejecutarse dentro de una transacción de base de datos.
- El historial de cambios se mantendrá dentro del proyecto bajo `.copilot/`.

## Resultado esperado
La documentación debe dejar claro el enfoque técnico y de negocio del módulo SPI para que el programador pueda continuar con la implementación en una base consistente y con estándares definidos.
