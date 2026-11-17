# Portable deployment

The application can run on a container host or a server with Java 17. No hosting provider is required
by the source code. Keep PostgreSQL and MongoDB persistent and independent of the API container.

## Container image

```sh
docker build -t underart-backend .
docker run --env-file .env -e SPRING_PROFILES_ACTIVE=prod -p 8080:8080 underart-backend
```

Use reachable database hostnames inside a container; `localhost` refers to that container.
The image uses a non-root user, honors `PORT` and shuts down gracefully.

For a local container preview including both databases:

```sh
cp .env.example .env
docker compose -f docker-compose.yml -f compose.app.yml up --build -d
```

Set a random JWT secret before exposing the service. The Compose preview binds database ports to
loopback; only the API port is public. It does not configure production database authentication or TLS.

## Required settings

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | JDBC PostgreSQL URL, with TLS settings required by your database |
| `DB_USER`, `DB_PASSWORD` | Application database credentials |
| `MONGO_URI` | Authenticated MongoDB connection URI |
| `JWT_SECRET` | Random secret containing at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins |
| `PAYMENT_PROVIDER` | `simulated` for demonstrations, `stripe` for the Stripe adapter |
| `PAYMENT_CURRENCY` | Three-letter code; default `usd` |

Stripe requires `STRIPE_SECRET_KEY`. Media requires the `R2_*` variables in `.env.example`.
AI keys are optional. Configure bucket CORS to allow the frontend origin and the headers returned by
upload tickets. Configure HTTPS at your ingress and route traffic to `PORT`.

Use `/actuator/health/readiness` for readiness and `/actuator/health/liveness` for liveness.
Use `/actuator/health` when the deployment should also check both database connections.
Set `API_DOCS_ENABLED=true` only when you want public Swagger and OpenAPI endpoints.

## Migrating an existing installation

Back up PostgreSQL and MongoDB before upgrading. Flyway applies versioned SQL migrations at startup;
Hibernate validates the resulting schema. Do not edit migrations already applied to a database.
Migration V7 adds order snapshots, payment attempts, deadlines and the active-auction uniqueness rule.
If existing data has duplicate active auctions, resolve them before applying V7. Historical snapshot
values are backfilled from the current artwork and buyer records; earlier edits cannot be reconstructed.

Use the same currency as the previous installation when migrating. V7 backfills existing orders with
`usd`, the previous default. If the previous deployment used another currency, migrate those snapshots
before enabling payments. Do not change the configured currency while unpaid orders exist.

## Operating payments

A payment attempt is committed before contacting the provider. Interrupted requests retain the same
reference, method and amount. A retry within 23 hours reuses that attempt. Definitive declines allow a
new attempt; unresolved payments never expire or release their artwork automatically.

For older unresolved attempts, inspect the provider using the saved `payment_reference`. Reconcile
with the provider before making any database correction. There is no administrative reconciliation UI
or webhook handler yet. Do not enable real-money payments before verifying the provider integration
and the interactive authentication flow required by your frontend.

Start with one API instance because rate limits are in memory. Background auction and order updates
use database locks, but notifications have best-effort delivery. Store secrets in the host's secret
manager, keep database backups and retain application logs with request IDs.
