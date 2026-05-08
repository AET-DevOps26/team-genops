# GenOps — Job Application Tracker

A web application that helps job seekers manage and prepare for their job search:

- **Track applications** across stages (Applied, Interview, Offer, ...).
- **Connect an email account** to surface AI-generated insights and recommended next steps.
- **Generate tailored cover letters** from a filled-in profile and target role.
- **Get role-fit analysis** highlighting strengths and gaps versus the job description.
- **Drill into insights** with chat-based follow-up questions.

## Architecture

Five services, one frontend. See [`docs/architecture/microservice-design.md`](docs/architecture/microservice-design.md) for the full design with bounded contexts, data ownership, communication patterns, and trade-offs.

| Service | Stack | Responsibility |
|---|---|---|
| `auth-service` | Spring Boot | Identity, JWT issuance |
| `application-service` | Spring Boot | Application CRUD + Recommendations |
| `email-service` | Spring Boot | Gmail/Outlook integration |
| `document-service` | Spring Boot | Profiles + Cover letters |
| `genai-service` | Python / FastAPI | LLM-backed AI features |
| `web-client` | React + TypeScript | UI |

Single Postgres instance with schema-per-service. Auth at the gateway; JWT verified at every hop. OpenAPI-first contract in [`api/openapi.yaml`](api/openapi.yaml).

## Repository Layout

```
api/openapi.yaml        Single source of truth for all REST contracts
services/               Spring Boot + FastAPI service code
web-client/             React + TypeScript frontend
infra/                  Kubernetes manifests / Helm charts
monitoring/             Prometheus rules + Grafana dashboard JSON
.github/workflows/      CI/CD pipelines
docs/
  architecture/         Subsystem decomposition, AOM, use case diagrams
  requirements/         Problem statement + project requirements
```

## Quick Start

> Local stack runs in three commands or fewer once `docker-compose.yml` lands. Currently scaffolding.

```bash
docker compose up
```

## Documentation

- [Microservice design](docs/architecture/microservice-design.md) — service boundaries, data ownership, orchestration patterns
- [Component diagram](docs/architecture/componentDiagram.png)
- [Use case diagram](docs/architecture/useCaseDiagram.png)
- [Analysis Object Model](docs/architecture/analysisObjectModel.png)
- [Requirements](docs/requirements/requirements.md)
- [Problem statement](docs/requirements/problem_statement_template.md)

## Tech Stack

React · Spring Boot (4 services) · Python / FastAPI · PostgreSQL · Docker Compose · Kubernetes / Helm · GitHub Actions · Prometheus + Grafana.

LLM backend selectable via `LLM_BACKEND` env var: `cloud` (OpenAI API) or `local` (LLaMA / GPT4All via Ollama).

## Team

Three students; each owns one primary subsystem (frontend, backend, GenAI) but contributes across boundaries on integration, deployment, and observability.
