# Contributing

Use Java 17 and the Maven Wrapper. Run `./mvnw clean verify` with Docker before submitting changes.

Keep source code, API messages and tests in English. Prefer short methods and descriptive names over
comments. Keep domain code independent of frameworks and infrastructure. Introduce patterns only
when they solve a concrete problem.

Add regression tests for business rules and concurrency. Keep existing coverage thresholds. Add new
Flyway migrations instead of changing applied migrations. Never commit credentials or `.env` files.
Update API examples and deployment notes when their contracts change.
