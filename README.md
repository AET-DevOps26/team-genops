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

Single Postgres instance with schema-per-service. Auth at the gateway; JWT verified at every hop. OpenAPI-first contract in [`api/openapi.yaml`](api/openapi.yaml).

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

```bash
# Copy env file and fill in values
cp .env.example .env

# Start everything
docker compose up --build

# Start a single service
docker compose up --build auth
docker compose up --build web-client
```

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Auth API | http://localhost:8080 |
| Postgres | localhost:5432 |
| pgAdmin | http://localhost:5050 |

**Frontend dev mode** (hot reload, no Docker):
```bash
cd web-client && npm install && npm run dev
```

**Auth service dev mode:**
```bash
cd services/auth && ./mvnw spring-boot:run
```

**OpenAPI codegen** (run from `api/`):
```bash
make generate   # regenerate all stubs + TS types
make lint       # lint openapi.yaml
make check      # CI drift check
```

## Tech Stack

React · Spring Boot · Python / FastAPI · PostgreSQL · Redis · Docker Compose · Kubernetes / Helm · GitHub Actions · Prometheus + Grafana.

LLM backend selectable via `LLM_BACKEND` env var: `cloud` (OpenAI API) or `local` (LLaMA / GPT4All via Ollama).

## Team




