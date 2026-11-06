# 0002. PostgreSQL para el negocio, MongoDB para eventos y Flyway para el esquema

- **Estado:** aceptada
- **Fecha:** 2026-10-17

## Contexto

Las pujas, órdenes y subastas necesitan transacciones, bloqueos de fila y consultas con joins y agregados.
Los registros de actividad, el historial de respuestas de la IA y las notificaciones son documentos de solo
añadir, con forma variable y mucho volumen. Además, el esquema se creaba con `ddl-auto: update`, que no
versiona cambios ni sirve para producción.

## Decisión

- **PostgreSQL** guarda usuarios, obras, multimedia, subastas, pujas, órdenes, favoritos, reseñas y refresh tokens.
- **MongoDB** guarda la actividad, las generaciones de IA y las notificaciones (con índice compuesto por
  usuario, estado de lectura y fecha).
- **Flyway** versiona el esquema relacional (`V1` a `V6`). Hibernate solo **valida** (`ddl-auto: validate`).
  `V1` reproduce exactamente el esquema que había creado Hibernate, y `baseline-on-migrate` permite adoptar
  una base existente sin perder datos.
- Las operaciones que compiten (pujar, comprar ya, cancelar, cerrar) bloquean la fila de la subasta
  (`SELECT ... FOR UPDATE`) dentro de una transacción.

## Consecuencias

- Cada cambio de esquema es un archivo revisable, y la app se niega a arrancar si el esquema no coincide
  con las entidades.
- Las pruebas de integración usan PostgreSQL y MongoDB reales con Testcontainers.
- No hay transacciones entre las dos bases: si falla la escritura en PostgreSQL después de escribir en
  MongoDB, puede quedar un registro huérfano de actividad o de IA. Lo aceptamos porque esos datos son
  informativos; las notificaciones se envían en modo "mejor esfuerzo" y nunca rompen la operación principal.
- La app necesita MongoDB al arrancar para crear los índices; si no responde, el arranque falla en ~30 s.
