-- ========================
-- SCHEMA: genai
-- ========================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enable pgvector extension (required for embedding column)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS genai;

CREATE TABLE IF NOT EXISTS genai.chat_sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    application_id UUID,
    session_type   VARCHAR(50) NOT NULL CHECK (session_type IN ('mock_interview', 'cover_letter_chat', 'fit_analysis_chat', 'insight_chat')),
    summary        TEXT,
    embedding      vector(4096),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Databases created before application_id existed still lack the column: CREATE TABLE
-- IF NOT EXISTS above is a no-op for them.
ALTER TABLE genai.chat_sessions ADD COLUMN IF NOT EXISTS application_id UUID;

-- Mock-interview state. Only mock_interview sessions use these; they stay NULL for every
-- other session_type. status tracks the interview lifecycle, score is the final (possibly
-- early-exit-penalised) 0-100 result, and evaluation holds the full structured breakdown.
ALTER TABLE genai.chat_sessions ADD COLUMN IF NOT EXISTS interview_status VARCHAR(20)
    CHECK (interview_status IS NULL OR interview_status IN ('in_progress', 'completed'));
ALTER TABLE genai.chat_sessions ADD COLUMN IF NOT EXISTS interview_score INTEGER;
ALTER TABLE genai.chat_sessions ADD COLUMN IF NOT EXISTS interview_evaluation JSONB;

-- Migration: adopt a new embedding dimension if the model changed.
--
-- This file is re-executed on every service start, so the guard must compare the CURRENT
-- dimension and do nothing when it already matches. It previously fired whenever the column
-- merely existed — which is always — so every restart silently dropped every stored embedding
-- and cross-session recall could never find anything (summaries survived; their vectors did not).
--
-- pgvector stores the declared dimension in atttypmod.
DO $$
DECLARE
    current_dims integer;
BEGIN
    SELECT atttypmod INTO current_dims
    FROM pg_attribute
    WHERE attrelid = 'genai.chat_sessions'::regclass
      AND attname = 'embedding'
      AND NOT attisdropped;

    IF current_dims IS NOT NULL AND current_dims <> 4096 THEN
        -- Dimensions actually changed: existing vectors are unusable, so drop and recreate.
        -- Summaries are kept and will be re-embedded on the next summarization.
        ALTER TABLE genai.chat_sessions DROP COLUMN embedding;
        ALTER TABLE genai.chat_sessions ADD COLUMN embedding vector(4096);
    END IF;
END $$;

-- Note: ivfflat and hnsw indexes are limited to 2000 dimensions.
-- qwen3-embedding-8b produces 4096-dim vectors so we rely on sequential scan.
-- For a single user's sessions this is negligible. Add a quantized index if scale demands it.
CREATE INDEX IF NOT EXISTS chat_sessions_user_id_idx ON genai.chat_sessions (user_id);

CREATE TABLE IF NOT EXISTS genai.chat_messages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seq        BIGINT GENERATED ALWAYS AS IDENTITY,
    session_id UUID NOT NULL REFERENCES genai.chat_sessions(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Migration: add seq column to existing tables that lack it
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'genai'
          AND table_name = 'chat_messages'
          AND column_name = 'seq'
    ) THEN
        ALTER TABLE genai.chat_messages
            ADD COLUMN seq BIGINT GENERATED ALWAYS AS IDENTITY;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS genai.user_memory (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL UNIQUE,
    summary    TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
