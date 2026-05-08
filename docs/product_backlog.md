# Product Backlog — JobReady (Version 3.0.0)

> Items are grouped by subsystem.
> Bonus features are optional and only to be attempted after all core items are complete.

---

## Legend

| Field | Values |
|---|---|
| Type | `Feature` · `Task` · `Bonus Feature` |
| Component | `Client` · `Server` · `GenAI` · `Infra` · `All` |
| Status | `To Do` · `In Progress` · `Done` |

---

## Infrastructure & DevOps

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-I01 | Set up GitHub mono-repository with folder structure (client/, server/, genai/, k8s/, docs/) | Task | All | To Do |
| PROJ-I02 | Write Dockerfile for each service (client, each Spring Boot microservice, GenAI) | Task | All | To Do |
| PROJ-I03 | Write `docker-compose.yml` to run the full system locally end-to-end with sane defaults (≤3 commands from clone to running) | Task | Infra | To Do |
| PROJ-I04 | Implement GitHub Actions CI pipeline (build + test on every PR) | Task | Infra | To Do |
| PROJ-I06 | Add static analysis / linting step to CI pipeline | Task | Infra | To Do |
| PROJ-I07 | Implement GitHub Actions CD pipeline (auto-deploy to Kubernetes on merge to main) | Task | Infra | To Do |
| PROJ-I08 | Write Kubernetes manifests or Helm charts for all services | Task | Infra | To Do |
| PROJ-I09 | Configure environment variables, Secrets, and ConfigMaps (no hardcoded credentials) | Task | Infra | To Do |
| PROJ-I10 | Deploy system to Rancher (course infrastructure) | Task | Infra | To Do |
| PROJ-I11 | Deploy system to Azure (cloud environment) | Task | Infra | To Do |
| PROJ-I12 | Configure Prometheus metrics collection (request count, latency, error rate) | Task | Infra | To Do |
| PROJ-I13 | Build Grafana dashboards for server and GenAI metrics; export as `.json` | Task | Infra | To Do |
| PROJ-I14 | Define at least one Grafana alert rule (e.g. service down, slow response) | Task | Infra | To Do |
| PROJ-I15 | Write root `README.md` (setup guide, architecture, API docs, CI/CD, monitoring, student responsibilities) | Task | All | To Do |

---

## Database

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-D01 | Design and document full PostgreSQL schema (User, UserProfile, JobApplication, Resume, CoverLetter, FitAnalysis, InterviewSession, EmailConnection, Email) | Task | Server | To Do |
| PROJ-D02 | Write and version schema migrations (Flyway or Liquibase) | Task | Server | To Do |

---

## Server — Spring Boot Microservices

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-S01 | Define microservice decomposition and inter-service communication contracts | Task | Server | To Do |
| PROJ-S02 | Set up API Gateway (routing, CORS, auth filter) | Task | Server | To Do |
| PROJ-S03 | Implement user authentication service (register, login, JWT issuance and validation) | Feature | Server | To Do |
| PROJ-S04 | Implement user profile service (create, read, update profile; work experience, education, skills, languages, certifications) | Feature | Server | To Do |
| PROJ-S05 | Implement email integration service (connect/disconnect Gmail accounts, fetch and store emails) | Feature | Server | To Do |
| PROJ-S06 | Implement job application service (create application, store job description, manage status transitions) | Feature | Server | To Do |
| PROJ-S07 | Implement application tracking logic (detect application-related emails and link to correct JobApplication) | Feature | Server | To Do |
| PROJ-S08 | Implement follow-up detection logic (surface stale applications after a configurable number of days) | Feature | Server | To Do |
| PROJ-S09 | Implement document management service (store and retrieve generated resumes, cover letters, fit analyses) | Feature | Server | To Do |
| PROJ-S10 | Expose OpenAPI / Swagger documentation for all REST endpoints; serve Swagger UI | Task | Server | To Do |
| PROJ-S11 | Write unit and integration tests for critical server-side logic and REST endpoints | Task | Server | To Do |
| PROJ-S12 | Expose Prometheus metrics endpoint on each microservice | Task | Server | To Do |

---

## GenAI Service — Python

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-G01 | Define GenAI service architecture (agents, routing, model abstraction layer) | Task | GenAI | To Do |
| PROJ-G02 | Implement cloud LLM integration (OpenAI API or equivalent) | Task | GenAI | To Do |
| PROJ-G03 | Implement local LLM integration (GPT4All or LLaMA) | Task | GenAI | To Do |
| PROJ-G04 | Implement AI-powered tailored resume generation (UserProfile + job description → resume) | Feature | GenAI | To Do |
| PROJ-G05 | Implement AI-powered cover letter generation (UserProfile + job description → cover letter) | Feature | GenAI | To Do |
| PROJ-G06 | Implement candidate-role fit analysis (strengths mapping + gap identification) | Feature | GenAI | To Do |
| PROJ-G07 | Implement interview question generation (role- and company-specific question sets) | Feature | GenAI | To Do |
| PROJ-G08 | Implement chat-based mock interview (multi-turn conversation, structured feedback on clarity and completeness) | Feature | GenAI | To Do |
| PROJ-G09 | Implement AI-generated follow-up email drafting (based on application status and elapsed time) | Feature | GenAI | To Do |
| PROJ-G10 | Write unit tests for GenAI agent logic | Task | GenAI | To Do |
| PROJ-G11 | Expose Prometheus metrics from GenAI service (request count, latency, model errors) | Task | GenAI | To Do |
| PROJ-G12 | Implement full RAG setup with vector database (Weaviate) for context-aware generation; includes pgvector extension for embedding-based similarity search | Bonus Feature | GenAI | To Do |
| PROJ-G13 | Implement voice-based mock interview with delivery feedback (pacing, filler words, clarity) | Bonus Feature | GenAI | To Do |

---

## Client — Frontend

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-C01 | Set up frontend project (React / Angular / Vue.js) with routing and global layout | Task | Client | To Do |
| PROJ-C02 | Implement registration and login pages | Feature | Client | To Do |
| PROJ-C03 | Implement guided user profile setup flow (work experience, education, skills, certifications, languages) | Feature | Client | To Do |
| PROJ-C04 | Implement profile editing page | Feature | Client | To Do |
| PROJ-C05 | Implement email account connection UI (connect / disconnect Gmail) | Feature | Client | To Do |
| PROJ-C06 | Implement application tracking dashboard (list of applications by status, stage pipeline view) | Feature | Client | To Do |
| PROJ-C07 | Implement add-new-application flow (paste job description, trigger AI generation) | Feature | Client | To Do |
| PROJ-C08 | Implement application detail page (view resume, cover letter, fit analysis; update status) | Feature | Client | To Do |
| PROJ-C09 | Implement follow-up suggestion UI (surface recommendation and pre-drafted follow-up email) | Feature | Client | To Do |
| PROJ-C10 | Implement chat-based mock interview UI (multi-turn chat, per-answer feedback display) | Feature | Client | To Do |
| PROJ-C11 | Write client-side tests for core workflows (auth, profile setup, application flow) | Task | Client | To Do |

---

## Engineering Artefacts & Documentation

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-A01 | Create initial Use Case Diagram | Task | All | Done |
| PROJ-A02 | Create initial Subsystem Decomposition Diagram | Task | All | Done |
| PROJ-A03 | Create initial Analysis Object Model | Task | All | Done |
| PROJ-A04 | Create Problem Statement | Task | All | Done |
| PROJ-A05 | Document subsystem interfaces and API contracts | Task | All | To Do |

---

## Project Skills (Working Agreements)

> Established once at the start of the project and maintained throughout.

| Issue ID | Feature / Task | Type | Component | Status |
|---|---|---|---|---|
| PROJ-P01 | Define branching naming convention (e.g. `feature/PROJ-C01-short-description`, `fix/PROJ-S03-jwt-bug`) | Task | All | To Do |
| PROJ-P02 | Define commit message format (e.g. conventional commits: `feat:`, `fix:`, `chore:`, `docs:`) | Task | All | To Do |
| PROJ-P03 | Create PR template (description, linked issue, checklist: tests pass, linter clean, reviewed) | Task | All | To Do |
| PROJ-P04 | Define Definition of Done (tests written, CI green, PR approved, deployed to dev, docs updated) | Task | All | To Do |
| PROJ-P05 | Configure linter and code formatter per subsystem (ESLint/Prettier for client, Checkstyle for server, Ruff for GenAI) | Task | All | To Do |
