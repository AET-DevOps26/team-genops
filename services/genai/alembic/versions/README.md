# GenAI Service — Database Schema

Managed by **Alembic** (Python equivalent of Flyway). Migrations run on service startup.

## Schema: `genai`

### `genai.chat_sessions`
Stores chat sessions with a compressed summary and vector embedding for RAG retrieval.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Owner — from JWT `sub` claim only |
| `application_id` | UUID | Optional — links session to a job application |
| `session_type` | VARCHAR(50) | `mock_interview`, `cover_letter_chat`, `fit_analysis_chat`, `insight_chat` |
| `summary` | TEXT | AI-generated compression of the session — updated as messages slide out |
| `embedding` | vector(1536) | pgvector embedding of the session summary — used for RAG retrieval |
| `created_at` | TIMESTAMPTZ | Set on insert |
| `updated_at` | TIMESTAMPTZ | Updated when summary changes |

### `genai.chat_messages`
Sliding window of the last 10 messages for the active session.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `session_id` | UUID | References `chat_sessions.id` |
| `role` | VARCHAR(20) | `user` or `assistant` |
| `content` | TEXT | Message content |
| `created_at` | TIMESTAMPTZ | Set on insert |

**Sliding window rule:** Only the last 10 messages per session are kept. When a new message is added and the count exceeds 10, the oldest message is dropped and the session `summary` is updated to include the compressed context.

### `genai.user_memory`
Long-term compressed memory across all sessions for a user. One row per user.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Unique — one memory per user |
| `summary` | TEXT | Compressed memory across all past sessions |
| `updated_at` | TIMESTAMPTZ | Updated after each session ends |

## Three-Layer Memory Architecture

```
LLM context = user_memory.summary                     (long-term:  who the user is across all sessions)
            + top-k relevant session summaries (RAG)  (semantic:   most relevant past sessions via pgvector)
            + chat_sessions.summary                   (mid-term:   what happened earlier in this session)
            + last 10 chat_messages                   (short-term: the active conversation window)
```

## pgvector Index

An `ivfflat` index is created on `chat_sessions.embedding` for fast cosine similarity search:
```sql
CREATE INDEX ON genai.chat_sessions USING ivfflat (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
```
This enables efficient RAG retrieval of the most relevant past sessions for a user.

## Migration Files

| File | Description |
|---|---|
| `001_create_genai_schema.sql` | Creates `genai` schema, enables pgvector, creates all tables and indexes |

## NOTE: Alembic Setup Requirements

This is a Python/FastAPI service. Alembic is the migration tool (equivalent of Flyway for Spring Boot). The service owner must:

1. Add dependencies to `requirements.txt`:
   ```
   alembic
   sqlalchemy
   asyncpg
   pgvector
   ```

2. Initialise Alembic in the service root:
   ```sh
   alembic init alembic
   ```

3. Configure `alembic.ini` with the database URL:
   ```ini
   sqlalchemy.url = postgresql+asyncpg://${DB_USER}:${DB_PASSWORD}@${DB_HOST}/${DB_NAME}
   ```

4. Configure `alembic/env.py` to use the `genai` schema.

5. Ensure the PostgreSQL instance has the `pgvector` extension available (`pgvector/pgvector:pg16` image already used in `docker-compose.yml` ✅).
