# Microservice Architecture Rules

## Design
- Each service owns one domain; no overlapping responsibilities.
- Services must be stateless — store session/context in tokens (JWT) or a shared cache (Redis), never in-process.
- Cross-service communication via JSON over HTTP only; never share internal data structures.

## API Design
- Define the OpenAPI spec **before** writing any implementation code. Review collaboratively.
- Version all APIs from day one: `/api/v1/resource`.
- Use code generators — no hand-written DTOs:
  - Java stubs: `openapi-generator-cli generate -i api/openapi.yaml -g spring -o services/<svc>/generated`
  - Python client: `openapi-python-client --path api/openapi.yaml --output services/<svc>/client`
  - TypeScript types: `npx openapi-typescript api/openapi.yaml -o web-client/src/api.ts`
- Lint the spec on every change: `npx @redocly/cli lint api/openapi.yaml`. Block merges on lint failure (pre-commit hook).
- The OpenAPI spec lives once in `/api/openapi.yaml` — it is the single source of truth.

## Security
- The gateway (NGINX Gateway Fabric) authenticates ingress and **each service re-verifies** the JWT with the auth service's public key (defense in depth).
- **Browser edge:** tokens reach the browser only as HttpOnly cookies (`jr_access`, `jr_refresh`; `HttpOnly; Secure; SameSite=Strict`), set by the auth service. Never in a response body, never readable by JS.
- **Inside the mesh:** the gateway translates the `jr_access` cookie into an `Authorization: Bearer` header; services pass tokens via that header only — never cookies or query params between services.

## Error Handling
- All services use a unified error schema: `{ code, message, details }`. Enforce it in the OpenAPI spec.

## CI/CD
- CI must run: OpenAPI lint → code-gen → build → tests → contract tests (Pact) on every PR.
- Build and publish Docker images with semantic version tags **and** Git SHA: `ghcr.io/org/svc:1.0.0` / `ghcr.io/org/svc:sha-abc123`.
- No manual production deploys — everything through CI/CD.
- Keep branches short-lived: merge or rebase within 2 days.

## Testing
- Write integration and E2E tests that hit a real database and external APIs — not mocks — per service.
- Run Pact contract tests between producer and consumer on every build.

## What Not To Do
- No direct HTTP calls without a generated client.
- No shared DTOs or utilities outside the OpenAPI spec.
- No long-running feature branches (> 2 days).
- No manual production deploys.
