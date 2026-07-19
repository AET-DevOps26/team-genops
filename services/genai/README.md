# GenAI Service

> JobReady's AI engine — a FastAPI service hosting a LangGraph-powered career assistant with
> persistent chat sessions, cross-session memory (pgvector RAG), scored mock interviews, and
> LLM utilities consumed by the other services.

**Stack:** Python 3.12 · FastAPI · LangChain / LangGraph · OpenRouter (OpenAI-compatible) ·
PostgreSQL + pgvector · Langfuse · `uv`

---

## What it does

| Capability | How |
|---|---|
| **Career assistant chat** | Multi-turn sessions with the full profile + application context injected; agent tools let the LLM look up the user's job applications on demand |
| **Document generation** | Slash commands inside any chat: `/cover_letter`, `/resume_tailor`, `/fit_analysis` |
| **Mock interviews** | A dedicated `mock_interview` session type bound to a job application — preconditions enforced (job description + complete profile), interviewer opens the conversation, and `end-interview` returns a structured 0–100 evaluation (early exits are penalised) |
| **Cross-session memory** | Sessions are summarized in the background; summaries are embedded (pgvector, 4096-dim) and exposed to the agent as a `search_past_sessions` tool |
| **Job-posting extraction** | Fetches a public posting URL and extracts company / title / description for the application form (best-effort, falls back to manual entry) |
| **Email analysis** | Internal endpoint used by the email service's application-detection pipeline — classifies incoming mail as application updates |

## API

All user-facing routes require a Bearer JWT (verified against the auth service's JWKS;
`user_id` always comes from the token's `sub` claim, never from the request).

### Chat — `/api/v1/chat`

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/sessions` | Create a session (`insight_chat`, `cover_letter_chat`, `fit_analysis_chat`, `mock_interview`) |
| `GET` | `/sessions` | List the user's sessions |
| `DELETE` | `/sessions/{id}` | Delete a session |
| `GET` | `/sessions/{id}/messages` | Session transcript |
| `POST` | `/sessions/{id}/messages` | Send a message, get the assistant's reply |
| `POST` | `/sessions/{id}/end-interview` | Finish a mock interview → structured score + evaluation |

### Job postings — `/api/v1/job-postings`

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/extract` | Extract company / job title / description from a public posting URL |

### Internal — `/internal/v1` (service-to-service only)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/email-analysis` | LLM classification of an email for the detection pipeline |

Guarded by the static `INTERNAL_SERVICE_TOKEN` shared with the email service — a blank token
disables the internal API entirely (**fail-closed**). Never routed through the gateway.

Interactive docs at [`/docs`](http://localhost:8000/docs) when running.

## Architecture

```
src/
├── main.py                  # App wiring: lifespan (DB + tracing), metrics, routers
├── config.py                # Pydantic Settings — all env vars, resolves the repo-root .env
├── auth.py                  # JWKS-based JWT verification + internal-token guard
├── observability.py         # Langfuse tracing (no-op when keys are blank)
├── routers/                 # HTTP layer: chat, job_postings, internal_analysis
├── services/
│   ├── chat/                # Session lifecycle, LangGraph chain, mock-interview flow
│   │   └── utils/           # History windowing, summarizer, embeddings, RAG retrieval
│   ├── job_posting/         # Page fetcher + LLM extractor
│   ├── email_analysis.py    # Email classification for the detection pipeline
│   ├── profile_client.py    # Document service client (profile context)
│   └── application_client.py# Application service client (job/company context)
├── tools/                   # LangChain tools the agent can call
│   ├── applications.py      #   list/get the user's job applications
│   └── session_memory.py    #   search_past_sessions (pgvector similarity)
├── prompts/                 # All prompt text, by feature (career_assistant, interviewer, …)
├── models/                  # Pydantic request/response schemas
└── db/pool.py               # psycopg async pool + schema bootstrap

sql/schema.sql               # Idempotent schema (genai schema, pgvector) — applied on startup
tests/                       # pytest suite (auth, sessions, commands, interview, RAG, …)
```

**How a chat message flows:** request → JWT verified → session ownership checked → history
window + profile/application context + any slash-command instructions assembled → LangGraph
agent runs (may call tools: applications lookup, past-session search) → reply persisted →
background task summarizes and re-embeds the session once it crosses the message threshold.

**Database.** The service owns the `genai` schema inside the shared Postgres (pgvector image).
`sql/schema.sql` is idempotent and re-applied on every startup by `db/pool.py` — additive
changes go in as `IF NOT EXISTS` / guarded `DO $$` blocks, so there is no separate migration
runner.

## Configuration

Settings are loaded by `src/config.py` (pydantic-settings) from the environment, falling back
to the **repo-root `.env`** — the same file Docker Compose uses, so local runs need no extra
env file.

| Variable | Default | Purpose |
|---|---|---|
| `OPENROUTER_API_KEY` | — **(required)** | LLM + embeddings backend (OpenAI-compatible) |
| `OPENROUTER_MODEL` | `openai/gpt-oss-120b` | Chat model |
| `OPENROUTER_EMBEDDING_MODEL` | `qwen/qwen3-embedding-8b` | Embedding model (4096-dim — changing dimensions drops stored vectors, see `sql/schema.sql`) |
| `DATABASE_URL` | localhost dev URL | Postgres connection string |
| `AUTH_JWKS_URL` | `http://auth:8080/...` | Where to fetch auth's public keys |
| `DOCUMENT_SERVICE_URL` | `http://document:8080` | Profile context source |
| `APPLICATION_SERVICE_URL` | `http://application:8080` | Job/company context source |
| `PROFILE_ENABLED` | `false` | Inject profile data into prompts (compose sets `true`) |
| `INTERNAL_SERVICE_TOKEN` | *(blank)* | Guards `/internal/**`; blank = disabled (fail-closed) |
| `LANGFUSE_HOST` / `LANGFUSE_PUBLIC_KEY` / `LANGFUSE_SECRET_KEY` | *(blank keys)* | LLM tracing; blank keys make tracing a no-op |

## Running

### Via Docker Compose (recommended — full stack)

From the repo root:

```bash
docker compose up --build genai
```

### Standalone (hot reload)

Dependencies are managed with [`uv`](https://docs.astral.sh/uv/) (`pyproject.toml` + `uv.lock`):

```bash
cd services/genai
uv sync                                              # install deps (incl. dev group)
uv run uvicorn src.main:app --reload --port 8000
```

Requires a reachable Postgres (with pgvector) and, for authenticated routes, a running auth
service to serve the JWKS.

## Testing & Quality

```bash
uv run pytest                       # full suite
uv run pytest tests/test_chat_session.py   # single file
uv run pytest --cov=src             # with coverage
uv run ruff check .                 # lint
uv run mypy src                     # type check
```

The suite covers auth/JWT handling, session lifecycle and ownership, slash commands, the
mock-interview flow, summarization, RAG retrieval, job-posting extraction, email analysis, and
the service clients — external LLM/HTTP calls are faked at the boundary.

## Observability

- **Metrics** — Prometheus via `prometheus-fastapi-instrumentator` at `/metrics`, plus
  `app_info{version}` (the image tag injected by Helm) for release correlation in Grafana.
- **Traces** — every LLM call is traced with **Langfuse** (tokens, cost, latency), attributed
  by `user_id` / `session_id`. Self-hosted locally via the `monitoring` compose profile;
  Langfuse Cloud in deployed environments. See [`monitoring/README.md`](../../monitoring/README.md).
- **Health** — `GET /health`.
