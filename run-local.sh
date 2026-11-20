#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

export POSTGRES_PORT="${POSTGRES_PORT:-15432}"
export MONGO_PORT="${MONGO_PORT:-27017}"
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:${POSTGRES_PORT}/underart}"
export MONGO_URI="${MONGO_URI:-mongodb://localhost:${MONGO_PORT}/underart}"

npm --prefix frontend ci --no-audit --no-fund
VITE_CATALOG_CURRENCY="${VITE_CATALOG_CURRENCY:-${PAYMENT_CURRENCY:-usd}}" npm --prefix frontend run build -- --base=/app/
if [ -n "${SMTP_HOST:-}" ]; then
    docker compose up -d --wait postgres mongo
else
    export SMTP_PORT="${SMTP_PORT:-${MAILPIT_SMTP_PORT:-1025}}"
    export MAILPIT_SMTP_PORT="$SMTP_PORT"
    docker compose --profile dev-mail up -d --wait
    printf 'Verification inbox: http://localhost:%s/\n' "${MAILPIT_WEB_PORT:-8025}"
fi

printf 'Underart will be available at http://localhost:%s/app/\n' "${PORT:-8080}"
exec ./mvnw clean spring-boot:run "$@"
