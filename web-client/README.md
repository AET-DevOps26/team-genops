# Web Client

> JobReady's frontend — React + Vite + TypeScript + Tailwind CSS, with Redux Toolkit /
> RTK Query for state and data fetching.

The app talks to the API only at relative `/api/*` paths (same-origin). In dev, Vite proxies
`/api` to the target in `.env` (`VITE_API_TARGET`); in Docker/production, nginx forwards `/api`
to the gateway — so the browser never needs CORS and the HttpOnly auth cookies stay
first-party. API types are generated from [`api/openapi.yaml`](../api/openapi.yaml)
(`make -C api generate`) — never hand-written.

## Development

```sh
npm install
cp .env.example .env    # VITE_API_TARGET — default is correct for local dev
npm run dev             # dev server on http://localhost:5173
npm run build           # type-check + production build
npm run lint            # ESLint
```

## Testing

```sh
npm test                # unit/component tests (Vitest + Testing Library)
```

What's covered: the auth page flow (`src/pages/AuthPage.test.tsx`), auth state
(`src/services/auth/authSlice.test.ts`), API error mapping (`src/api/errors.test.ts`), and
application stage logic (`src/services/applications/stages.test.ts`). Tests run in jsdom
(`src/test/setup.ts`) and execute in CI on every PR. Full user journeys against the real
stack live in [`e2e_tests/`](../e2e_tests/).
