-- ========================
-- Index the processed_emails read path.
--
-- list_processed_emails filters by user_id and sorts by
--   received_at DESC NULLS LAST, processed_at DESC
-- The only existing index is UNIQUE (user_id, message_id), which can't serve that
-- sort, so Postgres sorts in memory on every page. This composite index covers the
-- filter + ordering so paginated reads stay index-ordered. Additive and idempotent.
-- ========================
CREATE INDEX IF NOT EXISTS ix_processed_emails_user_received
    ON email.processed_emails (user_id, received_at DESC NULLS LAST, processed_at DESC);
