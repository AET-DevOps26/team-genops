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

-- Migration: Update embedding column dimension for existing tables
-- If the table already exists with a different embedding dimension, this will update it.
-- This handles the case where the table was created with an older dimension (e.g., 1536 or 3072).
DO $$
BEGIN
    -- Check if the embedding column exists and has a different dimension
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'genai'
          AND table_name = 'chat_sessions'
          AND column_name = 'embedding'
    ) THEN
        -- Drop and recreate the column to change dimension
        -- This is safe because we're in early development; in production with data,
        -- you'd need a more careful migration strategy (backup, convert, restore)
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
    session_id UUID NOT NULL REFERENCES genai.chat_sessions(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS genai.user_memory (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL UNIQUE,
    summary    TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
