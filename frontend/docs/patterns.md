# Frontend design patterns

- **HTTP adapter** (`src/api/client.ts`). Converts browser requests and Spring REST responses into a consistent interface. It attaches bearer credentials, handles JSON, multipart bodies and empty responses, and preserves the backend's error details.
- **Session facade** (`src/api/client.ts`). Keeps session persistence and token rotation behind a small API. Concurrent unauthorized requests share one refresh request because each refresh token can be used once.
- **State-driven views** (`src/features/orders.ts`, `src/features/details.ts`). Available actions depend on the state and identity returned by the backend. For example, buyers can confirm receipt after the seller ships, and the bid form displays the server's minimum bid. The backend authorizes and validates each transition.

The router mounts each view into a new element. A slow response from a previous route can therefore only update its detached view. Features use native browser forms for keyboard interaction, validation and submit state.
