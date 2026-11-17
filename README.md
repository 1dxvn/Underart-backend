# Underart Backend

Java 17 / Spring Boot REST API for an art auction marketplace. Each artwork represents one unique piece.
PostgreSQL stores business data; MongoDB stores activity, AI history and notifications.

## Run locally

Requirements: Java 17 and Docker.

```sh
docker compose up -d
./mvnw spring-boot:run
```

The default `dev` profile uses local databases and a development JWT secret. AI credentials are optional:
artwork creation continues without suggestions when AI is unavailable. Media uploads require R2 credentials.
The demo account is disabled unless `DEMO_SEED=true` is explicitly set in development.

Use `.env.example` as a configuration reference. Compose reads `.env`; Java does not. Export variables
or configure them in your IDE when running the application directly.

- API: `http://localhost:8080/api/v1`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`
- Example requests: [docs/api.http](docs/api.http)

## Verify

```sh
./mvnw clean verify
```

Docker is needed for integration and concurrency tests. Without Docker, those tests are skipped.
ArchUnit verifies the layer boundaries. JaCoCo requires 80% line coverage and 70% branch coverage.
Reports are generated in `target/site/jacoco` and `target/surefire-reports`.

## Business rules

- Only the artist can manage artwork and create its auctions.
- An artwork can have one active auction. A database constraint also enforces this under concurrency.
- Active auctions and reserved or sold artworks cannot be edited, including their media.
- Buyers cannot bid on or buy their own artwork.
- The first bid can equal the base price; subsequent bids require a 5% increment, with a minimum of 1.00.
- Bids in the last two minutes extend the deadline by two minutes.
- Auction completion creates at most one order. Buy-now and bidding share the auction row lock.
- A pending order reserves the artwork. Unpaid orders expire after 48 hours and release it.
- Payments with an uncertain result remain `PROCESSING` and keep the artwork reserved.
- Paid artwork cannot be auctioned again. Expired orders cannot be paid.
- Orders retain artwork details, currency and the delivery address selected when payment starts.
- Total: subtotal + 15.00 shipping + a 10% platform fee. All amounts use the configured currency.
- Payment must precede shipping; delivery confirmation must precede a review.

## API conventions

UUID identifiers, UTC ISO-8601 dates and decimal monetary amounts. The API, validation and notifications
use English. Authenticate with `Authorization: Bearer <accessToken>`.

List responses use `{items, page, size, totalItems, totalPages}`. Pages start at zero; maximum size is 100.
Errors use `{status, error, message, path, requestId, timestamp, fieldErrors}`.
`X-Request-Id` and `Retry-After` are exposed to browser clients.

Access tokens last 15 minutes. Refresh tokens rotate and are stored hashed. Changing a password
invalidates existing access tokens and refresh tokens. Logout revokes the refresh-token family;
its access token can remain valid until expiration.

Order payment states: `PENDING`, `PROCESSING`, `PAID`, `FAILED`, `EXPIRED`.
Retry an interrupted payment using the same `POST /orders/{id}/pay` endpoint. The saved attempt reuses
its original reference and payment method. After 23 hours an unresolved attempt requires manual
provider reconciliation; it is never silently retried with a fresh key.

## Deployment and design

- [Portable deployment](docs/DEPLOYMENT.md)
- [Architecture and patterns](docs/PATTERNS.md)
- [Contribution guide](CONTRIBUTING.md)

The Docker image defaults to `prod`; configure database URLs, JWT secret, allowed origins and payment
provider explicitly. Production API documentation is disabled unless `API_DOCS_ENABLED=true`.

## Current limits

Activity and notifications are best effort and run after the SQL commit; they have no durable outbox.
Rate limits are in memory, so use one API instance until a shared limiter is introduced.
Stripe is an off-session integration: cards requiring interactive authentication need a future frontend
payment flow. External providers are tested with doubles, not live credentials. Artwork valuation is a
suggestion, not an appraisal or authenticity assessment.
