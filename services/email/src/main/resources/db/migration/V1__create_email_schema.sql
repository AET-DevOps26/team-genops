-- Email service schema. Tokens are encrypted at the application layer (AES-256-GCM via a JPA
-- AttributeConverter keyed by EMAIL_TOKEN_ENC_KEY), so no pgcrypto extension is needed.
CREATE SCHEMA IF NOT EXISTS email;

CREATE TABLE email.email_connections (
    id            uuid PRIMARY KEY,
    user_id       uuid NOT NULL UNIQUE,
    provider      varchar(50) NOT NULL CHECK (provider IN ('gmail', 'outlook')),
    email_address varchar(255) NOT NULL,
    access_token  text NOT NULL,
    refresh_token text NOT NULL,
    token_expiry  timestamptz NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE email.processed_emails (
    id           uuid PRIMARY KEY,
    user_id      uuid NOT NULL,
    message_id   varchar(255) NOT NULL,
    subject      text,
    sender       varchar(320),
    snippet      text,
    received_at  timestamptz,
    processed_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_processed_emails_user_message UNIQUE (user_id, message_id)
);

-- Covers the read path: the user's messages, newest first.
CREATE INDEX ix_processed_emails_user_received
    ON email.processed_emails (user_id, received_at DESC NULLS LAST, processed_at DESC);
