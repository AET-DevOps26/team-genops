-- ========================
-- SCHEMA: genai
-- ========================

-- Enable pgvector extension (required for embedding column)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS genai;

CREATE TABLE genai.chat_sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    application_id UUID,
    session_type   VARCHAR(50) NOT NULL CHECK (session_type IN ('mock_interview', 'cover_letter_chat', 'fit_analysis_chat', 'insight_chat')),
    summary        TEXT,
    embedding      vector(1536),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for pgvector similarity search on session embeddings per user
CREATE INDEX ON genai.chat_sessions USING ivfflat (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE genai.chat_messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE genai.user_memory (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL UNIQUE,
    summary    TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
