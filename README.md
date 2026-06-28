# JobReady — Team GenOps

> End-to-end job application and preparation hub — TUM AET DevOps 2026

JobReady consolidates the entire job search journey into a single intelligent platform: build a structured professional profile, track applications via email integration, generate tailored resumes/cover letters/fit analyses, and rehearse for interviews with AI-powered mock sessions.

---

## Backlog & Issue Conventions

GitHub Issues are the single source of truth for work items. Each issue follows the ID format defined below.

### Issue ID Prefixes

| Prefix | Subsystem |
|---|---|
| `PROJ-I##` | Infrastructure & DevOps |
| `PROJ-D##` | Database |
| `PROJ-S##` | Server — Spring Boot |
| `PROJ-G##` | GenAI Service |
| `PROJ-C##` | Client — Frontend |
| `PROJ-A##` | Engineering Artefacts & Documentation |
| `PROJ-P##` | Project Skills / Working Agreements |

### Labels

Issues are labelled by **type** (`task`, `feature`, `bonus`) and **component** (`infra`, `server`, `genai`, `client`, `planning`).


## Architecture

Five services, one frontend. See [`docs/architecture/microservice-design.md`](docs/architecture/microservice-design.md) for the full design with bounded contexts, data ownership, communication patterns, and trade-offs.

| Service | Stack | Responsibility |
|---|---|---|
| `auth` | Spring Boot | Identity, JWT issuance |
| `application` | Spring Boot | Application CRUD + Recommendations |
| `email` | Spring Boot | Gmail/Outlook integration |
| `document` | Spring Boot | Profiles + Cover letters |
| `genai` | Python / FastAPI | LLM-backed AI features |
| `web-client` | React + TypeScript | UI |

Single Postgres instance with one database per service. JWTs are delivered to the browser as HttpOnly cookies (BFF / split-token); the gateway translates them into Bearer headers and JWT is verified at every hop. OpenAPI-first contract in [`api/openapi.yaml`](api/openapi.yaml).

## Repository Layout

```
api/openapi.yaml         Single source of truth for all REST contracts
services/               Spring Boot + FastAPI service code
web-client/             React + TypeScript frontend
infra/                  Kubernetes manifests / Helm charts
monitoring/             Prometheus rules + Grafana dashboard JSON
.github/workflows/      CI/CD pipelines
docs/
  architecture/         Subsystem decomposition, AOM, use case diagrams
  problem-statement/    Problem statement
  project_requirements.md  Requirements document
```


## Documentation

- [Microservice design](docs/architecture/microservice_design.md) — service boundaries, data ownership, orchestration patterns
- [Component diagram](docs/architecture/component_diagram.png)
- [Use case diagram](docs/architecture/use_case_diagram.png)
- [Analysis Object Model](docs/architecture/analysis_object_model.png)
- [Requirements](docs/project_requirements.md)
- [Problem statement](docs/problem-statement/problem_statement.md)

## Local Development

**Prerequisites:** Docker, Docker Compose, Java 21, Node 24, Maven.

### First-time setup

```bash
# 1. Root env — credentials for Postgres and pgAdmin
cp .env.example .env
# Fill in: POSTGRES_USER, POSTGRES_PASSWORD, PGADMIN_EMAIL, PGADMIN_PASSWORD
# POSTGRES_DB=auth_db is already set

# 2. Web client env — API base URL (default is correct for local dev)
cp web-client/.env.example web-client/.env

# 3. Codegen tooling — only needed when regenerating from openapi.yaml
npm ci --prefix api/scripts
make -C api generate   # skip if generated files are already committed

# 4. Git hooks — runs file hygiene checks before every commit
pip install pre-commit
pre-commit install
```

### Start the stack

```bash
# Full stack
docker compose up --build

# Single service
docker compose up --build auth
docker compose up --build web-client
docker compose up --build postgres-db
```

### OpenAPI codegen

```bash
make -C api generate   # regenerate all stubs + TS types
make -C api lint       # lint openapi.yaml
make -C api check      # CI drift check (fails if generated code is out of sync)
```


# Fresh database (wipes postgres volume — use after changing POSTGRES_DB)
docker compose down -v && docker compose up --build
```

### Service URLs

| Service | URL | Notes |
|---|---|---|
| Frontend | http://localhost:5173 | React + Vite |
| Auth API | http://localhost:8080 | Spring Boot |
| Auth Swagger UI | http://localhost:8080/swagger-ui.html | Interactive API docs |
| Auth Health | http://localhost:8080/actuator/health | Liveness check |
| Postgres | localhost:5432 | Direct DB access |
| pgAdmin | http://localhost:5050 | DB admin UI |

### pgAdmin — first-time server registration

pgAdmin has no servers pre-configured. Add one manually after first login:

1. Right-click **Servers** → **Register** → **Server…**
2. **General** tab → Name: `genops`
3. **Connection** tab:
   - Host: `postgres` · Port: `5432`
   - Database: `auth_db` · Username/Password: your `.env` values
4. Click **Save**

### Running services without Docker

**Frontend** (hot reload):
```bash
cd web-client && npm install && npm run dev
```

**Auth service** (requires a running Postgres on localhost:5432):
```bash
# Fill in services/auth/application-local.properties first
cd services/auth && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```


## Tech Stack

React · Spring Boot · Python / FastAPI · PostgreSQL · Redis · Docker Compose · Kubernetes / Helm · GitHub Actions · Prometheus + Grafana.

LLM backend selectable via `LLM_BACKEND` env var: `cloud` (OpenAI API) or `local` (LLaMA / GPT4All via Ollama).

## Team




