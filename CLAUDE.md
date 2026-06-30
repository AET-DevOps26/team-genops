# CLAUDE.md

This is the **team-wide** Claude Code configuration. It is committed to the repository and applies to every team member who opens this project in Claude Code. Do not put personal or machine-specific settings here — those belong in `.claude/settings.local.json` (gitignored).

## Project Overview

**JobReady** — an end-to-end job application and preparation hub built as a cloud-native, microservices-based system. This is a mono-repository for a TUM AET DevOps 2026 course project.

Core capabilities: structured job-profile management, application tracking (including email integration), AI-generated cover letters/fit analysis, and AI-powered insight chat.

## Architecture

The system is composed of five services in a single mono-repo:

| Service | Language | Bounded Context | Port |
|---|---|---|---|
| `auth` | Spring Boot | Identity — credentials, JWT issuance | 8080 |
| `application` | Spring Boot | Job application tracking + recommendations | 8082 |
| `document` | Spring Boot | Document storage — Profile, CoverLetter | TBD |
| `email` | Python / FastAPI | Email integration (Gmail/Outlook adapter) | 8001 |
| `genai` | Python / FastAPI | GenAI generation — stateless LLM calls | 8000 |

One shared PostgreSQL instance with schema-per-service isolation (each service has its own DB user with access only to its schema). Each service owns its schema migrations in `src/main/resources/db/migration/` (Spring Boot/Flyway) or `alembic/versions/` (Python services — raw `.sql` files applied on startup by a small version-tracked runner).

> **Note:** `email` was moved from Spring Boot to Python/FastAPI. OAuth2 integration with Gmail and Outlook is significantly easier with Python libraries (`google-auth-oauthlib`, `msal`). The project still meets the requirement of at least 3 Spring Boot microservices (`auth`, `application`, `document`).

**Frontend:** React + Vite + TypeScript + Tailwind CSS (`web-client/`, port 5173)

**OpenAPI:** Single source of truth at `api/openapi.yaml`. All DTOs are generated — no hand-written request/response classes. Re-run codegen after any endpoint change: `make -C api generate`.

All services are containerised and orchestrated locally with **Docker Compose** and deployed to **Kubernetes** (Helm or raw manifests).

**Observability stack:** Prometheus (metrics: request count, latency, error rate) + Grafana (dashboards committed as `.json` files) with alert rules for service health.

## Authentication

- **Token transport — HttpOnly cookies (BFF / split-token).** On login/register the auth service sets two cookies — `jr_access` (the access JWT, `Path=/`) and `jr_refresh` (`Path=/api/v1/auth`), both `HttpOnly; Secure; SameSite=Strict`. Tokens are **never** returned in a response body and **never** readable by JS. The browser only ever talks to its own origin; the proxy (Vite in dev, the gateway in prod) forwards `/api` to auth. *Why:* keeping cookies first-party lets `SameSite=Strict` work without CORS, and tokens stay out of JS (XSS-safe).
- **No CORS.** Because the browser reaches auth only same-origin via that proxy/gateway, cross-origin browser requests never happen, so the CORS config was inert and has been removed. *Why:* CORS is not the security boundary here — `SameSite=Strict` cookies + the gateway are. Reintroduce only if a real cross-origin browser client ever appears.
- **JWT signing keys.** Auth signs tokens with RSA (RS256) and publishes its public key as a JWK Set at `/api/v1/auth/.well-known/jwks.json` so other services verify locally. If `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` are blank, auth generates an **ephemeral in-memory key pair at startup** — convenient for single-instance dev. *Why it matters:* ephemeral keys regenerate on every restart (invalidating existing tokens) and differ across replicas (a token signed by one pod fails on another), so **production / multi-replica MUST set explicit keys**.

## Service Ports (local)

| Service    | URL                                   |
|------------|---------------------------------------|
| web-client | http://localhost:5173                 |
| auth       | http://localhost:8080                 |
| auth Swagger | http://localhost:8080/swagger-ui.html |
| application | http://localhost:8082                 |
| email      | http://localhost:8001                 |
| postgres   | localhost:5432                        |
| pgadmin    | http://localhost:5050 (dev only)      |

## Development Workflow

### Local setup

```sh
cp .env.example .env      # fill in secrets
docker compose up --build
```

Required env vars (see `.env.example`): `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`.

### Branch naming

```
PROJ-<issue-id>/<short-description>
```

Examples: `PROJ-I12/add-cover-letter-endpoint`, `PROJ-S04/fix-jwt-refresh`, `PROJ-P01/define-branching-convention`

The issue ID comes from the GitHub issue title prefix (e.g. `PROJ-C01`, `PROJ-S03`, `PROJ-I05`). Use kebab-case for the description. No `feature/` or `fix/` prefixes.

### Commit message format

Follow **Conventional Commits**:

```
<type>: <short imperative summary (≤72 chars)>

[optional body — explain why, not what]
```

**Allowed types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `infra`

Examples:
```
feat: add JWT verification to document service
fix: return 401 instead of 500 on expired token
test: add unit tests for CoverLetterService
ci: add lint step to PR workflow
docs: update OpenAPI spec for /applications endpoint
```

Use the imperative mood ("add", not "adds" or "added"). No AI attribution lines.

### CI/CD
- GitHub Actions on every PR: build → test → lint
- Merge to `main` → automatic deploy to Kubernetes
- Feature branch → PR → peer review + approval → merge (no direct pushes to `main`)

### Backend (Spring Boot)

Run from inside the service directory (e.g. `services/auth`):
```sh
./mvnw test                  # run all tests
./mvnw test -Dtest=ClassName # run a single test class
./mvnw spring-boot:run       # start locally
./mvnw verify                # build + test + package (mirrors CI)
```

Each microservice exposes OpenAPI docs at `/swagger-ui.html` and `/v3/api-docs`.

### Python services (genai + email)

```sh
cd services/genai   # or services/email
pip install -r requirements.txt
uvicorn src.main:app --reload --port 8000  # start locally
pytest                                      # run tests
pytest tests/test_foo.py                   # single file
```

### Frontend (React + Vite)

```sh
cd web-client
npm install
npm run dev      # start dev server (port 5173)
npm run build    # production build
npm run lint     # ESLint
npm test         # unit/component tests
```

### OpenAPI codegen

After any change to `api/openapi.yaml`:
```sh
make -C api generate
```

This regenerates Java DTOs for Spring Boot services and TypeScript types for the web client. Commit both the spec and the generated files. Lint blocks merge if the spec is invalid.

## Key Constraints

- **Reliability over features** — deploy early and iterate; a running subset beats a broken full system.
- **Early integration** — integrate components continuously, not at the last minute.
- **`user_id` from JWT only** — never accepted from request body, query string, or custom headers.
- **All CI steps must pass** before merging; no `--no-verify` bypasses.
- **Dashboards are code** — Grafana dashboard JSON files live in `monitoring/grafana/dashboards/`.
- **Secrets never committed** — use `.env` files (gitignored) or Kubernetes Secrets.
- **OpenAPI-first** — `api/openapi.yaml` is the single source of truth; re-run codegen after every endpoint change.

## Repository Layout

```
/
├── web-client/          # React + Vite + TypeScript frontend
├── services/
│   ├── auth/            # Spring Boot — Identity
│   ├── application/     # Spring Boot — Job application tracking
│   ├── document/        # Spring Boot — Profile & document storage
│   ├── email/           # Python FastAPI — Email integration (Gmail/Outlook)
│   └── genai/           # Python FastAPI — GenAI generation
├── api/
│   └── openapi.yaml     # Single source of truth for all REST contracts
├── infra/
│   └── k8s/             # Kubernetes manifests or Helm chart
├── monitoring/
│   ├── prometheus.yml   # Scrape config
│   ├── alerts/          # Alert rule YAML files
│   └── grafana/
│       └── dashboards/  # Exported Grafana dashboard JSON files
├── docs/
│   ├── architecture/    # Diagrams, microservice design
│   └── problem-statement/
├── docker-compose.yml
└── CLAUDE.md
```

## Claude Code Team Skills

This project ships with shared Claude Code skills. Invoke them with a `/` prefix in any Claude Code session.

| Skill | Description |
|---|---|
| `/run [service]` | Start the full stack (`docker compose up`) or a single service |
| `/test [service]` | Run tests for one or all services (mvnw, pytest, npm test) |
| `/lint [service]` | Lint one or all services (Checkstyle, Ruff, ESLint) |
| `/review [file\|PR]` | Code review against project standards and security rules |
| `/commit [hint]` | Create a commit following project message conventions |
| `/pr [title]` | Create a PR using the project template |
| `/generate-tests <file>` | Generate unit/integration tests for a given file or class |
| `/document [target]` | Update OpenAPI spec, README, or architecture notes |
| `/scaffold <type> <name>` | Scaffold a new Spring Boot microservice or GenAI FastAPI module |
| `/deploy [env]` | Validate and apply Kubernetes manifests (dry-run by default) |
| `/monitoring [task]` | Generate or validate Prometheus alert rules and Grafana dashboards |

Skill files live in `.claude/skills/<name>/SKILL.md`.

## Current State

Services scaffolded: `auth` (Spring Boot, functional with DB), `email` (Python/FastAPI, functional — Gmail OAuth2 connect/disconnect, background poller, JWT auth), `genai` (Python scaffold), `web-client` (React + Vite scaffold). Services `application` and `document` are not yet implemented beyond DB schema definitions.

PostgreSQL schemas designed and migration files written for all services:
- `auth` — schema auto-generated by Hibernate (`ddl-auto=create-drop`) from `@Entity` classes
- `document` — Flyway migration at `src/main/resources/db/migration/`
- `application` — Flyway migration at `src/main/resources/db/migration/`
- `email` — raw `.sql` migrations at `alembic/versions/`, applied on startup by `src/migrate.py`
- `genai` — Alembic migration at `alembic/versions/`

`docker-compose.yml` runs `auth` + `email` + `postgres` + `web-client`. Kubernetes manifests and monitoring config are pending.
