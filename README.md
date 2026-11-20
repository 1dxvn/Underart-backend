# Underart Backend

Spring Boot API and TypeScript frontend for an art auction marketplace. PostgreSQL stores business data.
MongoDB stores activity, AI history and notifications.

## Run locally

Requirements: Java 17, Node.js 22 and Docker with Compose.

```sh
./run-local.sh
```

Open [Underart](http://localhost:8080/app/). The script builds the frontend, starts the databases and launches Spring Boot.
The build is served by Spring from `/app/` and uses the same-origin `/api/v1` by default. The API status remains at `/`.
The script uses host ports 15432 for PostgreSQL and 27017 for MongoDB. Set `POSTGRES_PORT` or
`MONGO_PORT` before running it to use other ports. For a standalone API using the Compose defaults, run `docker compose up -d` and `./mvnw spring-boot:run`.

The default `dev` profile uses a development JWT secret, simulated payments and multipart media uploads stored in
`.underart-media/`. Set `LOCAL_MEDIA_DIRECTORY` to change the location. Set `MEDIA_STORAGE_PROVIDER=r2` and configure
the R2 variables to use Cloudflare or request presigned uploads. The production profile defaults to R2.
AI credentials are optional. Artwork creation continues without suggestions when AI is unavailable.

For IDE startup, build the frontend with `npm --prefix frontend ci` and
`npm --prefix frontend run build -- --base=/app/`, then run `./mvnw process-resources` so the browser assets are copied to the
Maven classpath. Start the databases and run `UnderartApplication` in the IDE.

See the [frontend demo and patterns](frontend/README.md) for the buyer and seller walkthrough.
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
- An auction can have multiple unpaid checkout attempts and at most one committed order.
- An award reserves the artwork. Unpaid orders expire after 48 hours. Opening checkout leaves the auction available.
- Payments with an uncertain result remain `PROCESSING` and keep the artwork reserved.
- Paid artwork cannot be auctioned again. Expired orders cannot be paid.
- Orders retain artwork details, currency and the delivery address selected when payment starts.
- Total: subtotal + weight-based estimated shipping + a 10% platform fee. The shipping estimate starts at
  COP 15,000 or USD/EUR 15 for up to 1 kg, then adds COP 3,000 or USD/EUR 3 per additional started kilogram.
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

## Registration and auction formats

Request a verification code before submitting registration. Codes expire after ten minutes and
allow five invalid attempts. Existing accounts can sign in as before. Development startup runs
Mailpit for local email capture at http://localhost:8025. To send to actual inboxes, export the SMTP
settings from `.env.example` with credentials and a verified From address for your SMTP provider.

- Open and buy-now formats allow the seller to accept the current leading bid before the deadline.
- Dutch auctions descend from the opening price to the floor across their scheduled duration.
  Accepting the current quote opens checkout at the server's calculated price.
- Each bidder gets one private bid in a sealed auction. Amounts stay hidden until the scheduled close;
  the highest amount wins, with the earlier bid winning a tie.
- Select USD, COP or EUR when publishing. Enter amounts in that currency. The order and its fees use
  the same currency; the application does not convert between currencies. COP auction prices and bids use increments of 1,000.

The verification use case uses persistence and mail ports with PostgreSQL and SMTP adapters.
Auction transitions reuse row locking and the same order factory and notification workflow.

Buy-now and Dutch checkouts leave the auction active until payment is approved. Cancelling or abandoning
checkout leaves the auction available. While the provider is processing a charge, other payments and
competing settlement are blocked until the result is known. Scheduled and seller-accepted awards still
close the bidding when the winner is selected.

## Artwork advice, profiles and card payments

The artwork form can request a description draft and an indicative value in the selected currency.
Set `GROQ_API_KEY` or `OPENAI_API_KEY` in the backend. The shipping amount is a deterministic estimate
by weight, without carrier or live route data. Paid orders retain their original amounts.

Users may upload a JPEG, PNG or WEBP avatar up to 5 MB and add a public bio. Local development stores
avatars on disk, and the production storage adapter uses R2.

For Stripe, export `PAYMENT_PROVIDER=stripe`, `STRIPE_SECRET_KEY` and `STRIPE_PUBLISHABLE_KEY` before
launching the backend. Use matching test keys first. Stripe.js sends card data directly to Stripe
and Underart receives a PaymentMethod identifier. The current backend uses off-session processing.
Cards needing interactive authentication require a different payment method until interactive
confirmation is implemented. This integration charges the platform account; seller payouts are not included.
