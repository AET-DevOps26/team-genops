# JobReady — Team GenOps

> An end-to-end job application and preparation hub, built as a cloud-native microservices system.
> TUM AET DevOps 2026.

[![CI](https://github.com/AET-DevOps26/team-genops/actions/workflows/ci.yml/badge.svg)](https://github.com/AET-DevOps26/team-genops/actions/workflows/ci.yml)
[![CD — Dev (AKS)](https://github.com/AET-DevOps26/team-genops/actions/workflows/cd-dev.yml/badge.svg)](https://github.com/AET-DevOps26/team-genops/actions/workflows/cd-dev.yml)
[![CodeQL](https://github.com/AET-DevOps26/team-genops/actions/workflows/codeql.yml/badge.svg)](https://github.com/AET-DevOps26/team-genops/actions/workflows/codeql.yml)

JobReady consolidates the job-search journey into a single intelligent platform:

- **Structured candidate profile** — work experience, education, skills, languages
- **Application tracking** — pipeline board with stages, recommendations, and an append-only event timeline
- **Email integration** — connect Gmail via OAuth2; an LLM pipeline detects application updates in incoming mail and moves the pipeline automatically
- **AI generation** — tailored cover letters, resumes, and fit analyses via a chat agent with RAG over past sessions
- **Insight chat & mock interviews** — AI-powered preparation, observable end-to-end via Langfuse

---

## Table of Contents

- [Architecture](#architecture)
- [Repository Layout](#repository-layout)
- [Getting Started (Docker Compose)](#getting-started-docker-compose)
  - [Prerequisites](#prerequisites)
  - [First-time setup](#first-time-setup)
  - [Configuring `.env`](#configuring-env)
  - [Start the stack](#start-the-stack)
  - [Service URLs](#service-urls)
- [Monitoring & Observability](#monitoring--observability)
- [Development](#development)
- [Testing](#testing)
- [CI/CD & Deployment](#cicd--deployment)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [Team](#team)

---

## Architecture

Six services and one frontend behind a single API gateway. See
[`docs/architecture/microservice_design.md`](docs/architecture/microservice_design.md) for the full
design — bounded contexts, data ownership, communication patterns, and trade-offs.

| Service | Stack | Port | Responsibility |
|---|---|---|---|
| `gateway` | Spring Cloud Gateway | 8081 | Edge — JWT validation, `/api` routing, cookie→Bearer translation |
| `auth` | Spring Boot | 8080 | Identity — registration/login, RS256 JWT issuance + JWKS, Redis-backed sessions |
| `application` | Spring Boot | 8082 | Application tracking CRUD, recommendations, per-application event timeline |
| `document` | Spring Boot | 8083 | Candidate profile + generated documents (cover letters, resumes) |
| `email` | Spring Boot | 8001 | Gmail OAuth2 integration + LLM application-detection pipeline |
| `genai` | Python / FastAPI | 8000 | LangGraph chat agent — sessions, pgvector RAG, document generation, Langfuse tracing |
| `web-client` | React + Vite + TypeScript | 5173 | UI — dashboard, applications board, profile editor, assistant chat |

**Supporting infrastructure:**

- **PostgreSQL** (pgvector/pg16) — one shared instance; each service owns its own database or schema and never touches another's. Migrations live with each service (Flyway for Spring, versioned SQL for genai).
- **Redis** — backs auth session/refresh-token handling.

**Security model.** The browser only ever talks to its own origin. JWTs are delivered exclusively as
`HttpOnly; Secure; SameSite=Strict` cookies (`jr_access`, `jr_refresh`) — never in a response body,
never readable by JS. The gateway validates the access JWT at the edge and translates the cookie into
an `Authorization: Bearer` header; every downstream service independently re-verifies the token
against auth's published JWKS (defense in depth). `user_id` is always taken from the verified JWT
`sub` claim, never from request input.

**API contract.** OpenAPI-first: [`api/openapi.yaml`](api/openapi.yaml) is the single source of truth
for all REST contracts. All DTOs (Java) and API types (TypeScript) are generated from it — no
hand-written request/response classes.

## Repository Layout

```
├── api/
│   ├── openapi.yaml          # Single source of truth for all REST contracts
│   └── Makefile              # Codegen: Java DTOs + TypeScript types (make -C api generate)
├── services/
│   ├── gateway/              # Spring Cloud Gateway — edge auth + routing
│   ├── auth/                 # Spring Boot — identity, JWT issuance
│   ├── application/          # Spring Boot — application tracking
│   ├── document/             # Spring Boot — profile & document storage
│   ├── email/                # Spring Boot — Gmail integration + detection pipeline
│   ├── genai/                # Python / FastAPI — LLM chat agent
│   └── db/initdb/            # Per-service database declarations (runs on first Postgres init)
├── web-client/               # React + Vite + TypeScript frontend
├── e2e_tests/                # Black-box end-to-end tests through the gateway (pytest)
├── infra/
│   ├── helm/jobready/        # Application Helm chart (AKS + TUM Rancher)
│   ├── helm/monitoring/      # Prometheus/Grafana chart — rules + dashboards (single source)
│   ├── terraform/            # Azure infrastructure provisioning
│   └── ansible/              # Deployment playbooks + vaulted secrets
├── monitoring/               # Opt-in local stack (Prometheus, Grafana, Langfuse)
├── docs/
│   ├── architecture/         # Microservice design, component/use-case diagrams, AOM
│   ├── problem-statement/    # Problem statement
│   ├── project_requirements.md
│   └── product_backlog.md
├── .github/workflows/        # CI, CD (dev/prod/monitoring), image builds, CodeQL
├── docker-compose.yml        # Local orchestration of the full stack
└── .env.example              # Documented template for all required configuration
```

## Getting Started (Docker Compose)

### Prerequisites

- **Docker** and **Docker Compose** — that's all you need to run the full stack.
- Only for running services *outside* Docker: JDK 21, Node 24, Maven, Python 3.11+.

### First-time setup

```bash
# 1. Clone and enter the repo
git clone https://github.com/AET-DevOps26/team-genops.git
cd team-genops

# 2. Create your env file from the documented template
cp .env.example .env

# 3. Generate the RSA JWT signing keys (one-time, REQUIRED).
#    The auth service refuses to start without them — there is deliberately
#    no auto-generated fallback (ephemeral keys would invalidate all tokens
#    on restart and break multi-replica deployments).
./scripts/gen-jwt-keys.sh >> .env

# 4. Web client env — API proxy target (the default is correct for local dev)
cp web-client/.env.example web-client/.env

# 5. Git hooks — auto-fix formatting / lint before every commit
pip install pre-commit
pre-commit install
```

### Configuring `.env`

`.env.example` is fully commented — every variable is explained inline. Reference:

| Variable(s) | Required | Purpose |
|---|---|---|
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | ✅ | Postgres credentials + the shared/maintenance database. Per-service databases (`auth_db`, `email_db`) are created automatically on first init by `services/db/initdb/` |
| `PGADMIN_EMAIL`, `PGADMIN_PASSWORD` | ✅ | pgAdmin login (local dev tool only) |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `JWT_KEY_ID` | ✅ | Auth's RS256 signing keys — filled in by `./scripts/gen-jwt-keys.sh` (setup step 3). Auth will not boot without them |
| `OPENROUTER_API_KEY` | ✅ for AI features | LLM backend for `genai` (OpenAI-compatible, via OpenRouter). `OPENROUTER_MODEL` / `OPENROUTER_EMBEDDING_MODEL` override the defaults |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | ⬜ Gmail | OAuth2 client from the Google Cloud console; the redirect URI must be registered there exactly. Blank = Gmail connect disabled |
| `EMAIL_TOKEN_ENC_KEY`, `STATE_SIGNING_KEY` | ⬜ Gmail | Encrypt stored OAuth tokens at rest (AES-GCM) / sign the short-lived OAuth `state`. Use strong random values; keep them distinct so they can rotate independently |
| `INTERNAL_SERVICE_TOKEN` | ⬜ | Shared secret for the email → genai/application detection pipeline. Blank disables the pipeline entirely (fail-closed) |
| `AUTH_REGISTER_MAX_ATTEMPTS` | ⬜ | Per-IP registration throttle (default 10 per 15 min). Raise (e.g. 1000) when running the e2e suite locally |
| `GRAFANA_ADMIN_PASSWORD` | ⬜ monitoring | Grafana `admin` password for the local monitoring profile (defaults to `admin`) |
| `LANGFUSE_HOST`, `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY` | ⬜ monitoring | LLM tracing for `genai`. Blank keys disable tracing; for local self-hosting any matching key pair works (Langfuse auto-creates the project on first boot) |

> **Minimum to run the core stack:** Postgres/pgAdmin credentials + the generated JWT keys +
> `OPENROUTER_API_KEY`. Everything else is opt-in and fails closed when blank — the stack starts
> fine without it, the corresponding feature is simply disabled.

### Start the stack

```bash
# Full stack — Postgres, Redis, all six services, frontend
docker compose up --build

# Detached, waiting until everything is healthy
docker compose up --build -d --wait

# A single service (its dependencies start automatically)
docker compose up --build auth

# Fresh database (wipes the Postgres volume — re-runs the per-service DB init)
docker compose down -v && docker compose up --build
```

Then open **http://localhost:5173** — register a user and you're in.

### Service URLs

| Component | URL | Notes |
|---|---|---|
| Frontend | http://localhost:5173 | nginx, proxies `/api` to the gateway |
| Gateway | http://localhost:8081 | The API edge — all `/api/**` traffic |
| Auth API | http://localhost:8080 | Swagger UI at `/swagger-ui.html` |
| Application API | http://localhost:8082 | Swagger UI at `/swagger-ui.html` |
| Document API | http://localhost:8083 | Swagger UI at `/swagger-ui.html` |
| Email API | http://localhost:8001 | Swagger UI at `/swagger-ui.html` |
| GenAI API | http://localhost:8000 | OpenAPI docs at `/docs` |
| PostgreSQL | `localhost:5432` | pgvector/pg16 |
| Redis | `localhost:6379` | Auth sessions/refresh |
| pgAdmin | http://localhost:5050 | Dev only — register the server manually (host `postgres-db`, port `5432`) |
| Prometheus | http://localhost:9090 | `monitoring` profile only |
| Grafana | http://localhost:3001 | `monitoring` profile only |
| Langfuse | http://localhost:3000 | `monitoring` profile only |

Each Spring service additionally exposes health and metrics on a separate **management port 8090**
(`/actuator/health`, `/actuator/prometheus`) — deliberately isolated from the public app port.

## Monitoring & Observability

The local monitoring stack (Prometheus + Grafana + Langfuse) is **opt-in** via a Compose profile.
Set `GRAFANA_ADMIN_PASSWORD` and the `LANGFUSE_*` variables in `.env`, then:

```bash
docker compose -f docker-compose.yml \
               -f monitoring/docker-compose.yml \
               --profile monitoring up
```

- **Metrics** — all Spring services emit RED metrics via Micrometer; `genai` via
  prometheus-fastapi-instrumentator. Custom auth security counters and `app_info{version}` on every
  service for release correlation.
- **Dashboards are code** — Grafana JSON lives once in `infra/helm/monitoring/files/dashboards/`;
  the prod chart and the local compose stack both consume the same files. Edit in git, never the UI.
- **Alert rules** — `infra/helm/monitoring/files/rules.yml` (`ServiceDown`, `HighErrorRate`,
  `SlowResponses`, `AuthLockoutSpike`), single-sourced for local and prod.
- **LLM observability** — Langfuse traces every `genai` LLM call with token usage and cost,
  attributed by `user_id`/`session_id`. Self-hosted locally, Langfuse Cloud in deployed environments.

Details: [`monitoring/README.md`](monitoring/README.md).

## Development

### Backend (Spring Boot) — run from the service directory

```bash
cd services/auth            # or application / document / email / gateway
./mvnw spring-boot:run      # start locally
./mvnw test                 # run tests
./mvnw verify               # build + test + package (mirrors CI — Spotless + Checkstyle)
./mvnw spotless:apply       # auto-fix formatting
```

> Java tooling requires **JDK 21** (`JAVA_HOME`) — palantir-java-format crashes on newer JDKs.

### GenAI (Python / FastAPI)

```bash
cd services/genai
uv sync                                            # deps from pyproject.toml + uv.lock
uv run uvicorn src.main:app --reload --port 8000
uv run pytest
```

See [`services/genai/README.md`](services/genai/README.md) for the service's API, architecture, and configuration.

### Frontend (React + Vite)

```bash
cd web-client
npm install
npm run dev      # dev server with hot reload (port 5173)
npm run build    # production build
npm run lint     # ESLint
npm test         # unit/component tests (Vitest)
```

### OpenAPI codegen

After **any** change to `api/openapi.yaml`:

```bash
make -C api generate   # regenerate Java DTOs + TypeScript types
make -C api lint       # lint the spec (blocks merge if invalid)
make -C api check      # CI drift check — fails if generated code is out of sync
```

Commit both the spec and the generated files.

## Testing

| Layer | Where | How |
|---|---|---|
| Unit + web-layer (Java) | `services/*/src/test` | `./mvnw test` per service |
| Unit (Python) | `services/genai/tests` | `pytest` |
| Unit/component (frontend) | `web-client/src/**/*.test.ts(x)` | `npm test` (Vitest) |
| End-to-end | [`e2e_tests/`](e2e_tests/) | Black-box, through the gateway — real services, real Postgres/Redis, nothing mocked |

```bash
# E2E: bring the stack up, then run the suite from the repo root
docker compose up -d --wait
pytest e2e_tests/ -v
```

> The e2e suite registers ~20 users from one IP — set `AUTH_REGISTER_MAX_ATTEMPTS=1000` in `.env`
> first or the auth throttle will return 429s.

## CI/CD & Deployment

- **CI** (`ci.yml`) — on every PR: OpenAPI lint + codegen drift check, build, tests, lint for all
  services. All checks must pass before merge; no direct pushes to `main`.
- **Images** (`build-images.yml`) — Docker images published to GHCR.
- **CD — Dev** (`cd-dev.yml`) — merge to `main` deploys automatically to **Azure AKS** via the
  [`infra/helm/jobready`](infra/helm/jobready) chart.
- **CD — Prod** (`cd-prod.yml`) — release-gated deployment to **TUM Rancher**.
- **CD — Monitoring** (`cd-monitoring.yml`) — changes under `infra/helm/monitoring/` deploy the
  Prometheus/Grafana stack to the `genops-monitoring` namespace.
- **CodeQL** (`codeql.yml`) — static security analysis.

Infrastructure is provisioned with **Terraform** (`infra/terraform/`) and configured/deployed with
**Ansible** (`infra/ansible/`, secrets in an Ansible vault). No manual production deploys.

## Documentation

- [Microservice design](docs/architecture/microservice_design.md) — service boundaries, data ownership, orchestration patterns
- [Component diagram](docs/architecture/component_diagram.png)
- [Use case diagram](docs/architecture/use_case_diagram.png)
- [Analysis Object Model](docs/architecture/analysis_object_model.png)
- [Requirements](docs/project_requirements.md)
- [Product backlog](docs/product_backlog.md)
- [Problem statement](docs/problem-statement/problem_statement.md)
- Per-service READMEs: [`services/gateway`](services/gateway/README.md) · [`services/application`](services/application/README.md) · [`services/document`](services/document/README.md) · [`services/email`](services/email/README.md) · [`services/genai`](services/genai/README.md) · [`e2e_tests`](e2e_tests/README.md) · [`monitoring`](monitoring/README.md)

## Contributing

GitHub Issues are the single source of truth for work items.

**Issue ID prefixes**

| Prefix | Subsystem |
|---|---|
| `PROJ-I##` | Infrastructure & DevOps |
| `PROJ-D##` | Database |
| `PROJ-S##` | Server — Spring Boot |
| `PROJ-G##` | GenAI Service |
| `PROJ-C##` | Client — Frontend |
| `PROJ-A##` | Engineering Artefacts & Documentation |
| `PROJ-P##` | Project Skills / Working Agreements |

Issues are labelled by **type** (`task`, `feature`, `bonus`) and **component** (`infra`, `server`,
`genai`, `client`, `planning`).

**Branches** — `PROJ-<issue-id>/<short-description>` (kebab-case), e.g.
`PROJ-I12/add-cover-letter-endpoint`. No `feature/` or `fix/` prefixes.

**Commits** — [Conventional Commits](https://www.conventionalcommits.org/), imperative mood:
`feat` · `fix` · `refactor` · `test` · `docs` · `chore` · `ci` · `infra`

```
fix: return 401 instead of 500 on expired token
```

**Workflow** — feature branch → PR → peer review + approval → merge. All CI checks must pass;
no `--no-verify` bypasses. Keep branches short-lived.

## Team

TUM AET DevOps 2026 — **Team GenOps**

| Name | GitHub |
|---|---|
| Georges Nasrallah | [@georges-nasrallah](https://github.com/georges-nasrallah) |
| Giang Vu | [@giang-h-vu](https://github.com/giang-h-vu) |
| Youssef El Toukhi | [@toukhi](https://github.com/toukhi) |
