# Underart Frontend

Public art and auction catalog with an authenticated selling and buying workflow. Built with TypeScript, semantic HTML, CSS and Vite.

## Frontend-only development

```sh
npm ci
cp .env.example .env.local
npm run dev
```

Open http://localhost:5173. Start Underart Backend separately, using its own README. The default API URL is http://localhost:8080/api/v1 and its CORS configuration allows the Vite development origin.

| Variable | Purpose |
| --- | --- |
| `VITE_API_BASE_URL` | Backend API root. |
| `VITE_CATALOG_CURRENCY` | Catalog price currency. Match the backend's `PAYMENT_CURRENCY`; the API returns currency in order snapshots. |

Use `PAYMENT_PROVIDER=simulated` on the backend. Checkout registers `tok_visa` and uses the backend payment endpoint. The dev backend stores media on disk by default. See the root README for running the integrated application and choosing a storage provider. Browser configuration variables are public.

## Demo

1. Browse works and auctions before signing in.
2. Request a code and use the local Mailpit inbox to verify registration. As the seller, create an artwork, upload a cover image and publish an auction with buy-now enabled.
3. Use a separate browser tab opened directly or a private window for the buyer. Register a second account, favorite the artwork, place a bid and use buy-now.
4. Open the pending order, enter a delivery address and confirm the simulated payment.
5. In the seller account, open Sales and add a carrier and tracking number.
6. In the buyer account, open Purchases or Shipping and confirm delivery. Visit Notifications to review and mark messages as read.
7. To demonstrate the winning-bid path, let a second auction close with a valid bid, then open the resulting order in Purchases.

`npm run check` checks types. `npm run build` creates the static site in `dist`.

See [design patterns](docs/patterns.md) for the architecture discussion.
