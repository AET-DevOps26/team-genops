-- ========================
-- SCHEMA: email
-- ========================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS email;

CREATE TABLE email.email_connections (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL UNIQUE,
    provider      VARCHAR(50) NOT NULL CHECK (provider IN ('gmail', 'outlook')),
    email_address VARCHAR(255) NOT NULL,
    access_token  TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    token_expiry  TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE email.processed_emails (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    message_id   VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, message_id)
);
