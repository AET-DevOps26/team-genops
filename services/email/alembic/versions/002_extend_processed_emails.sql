-- ========================
-- Extend email.processed_emails with fetched content.
--
-- 001 only tracked message_id for dedupe. "Fetch AND store emails" needs the content,
-- so add the columns the poller persists. Additive and idempotent (IF NOT EXISTS);
-- the existing UNIQUE (user_id, message_id) dedupe constraint is left untouched.
-- ========================
ALTER TABLE email.processed_emails ADD COLUMN IF NOT EXISTS subject     TEXT;
ALTER TABLE email.processed_emails ADD COLUMN IF NOT EXISTS sender      VARCHAR(320);
ALTER TABLE email.processed_emails ADD COLUMN IF NOT EXISTS snippet     TEXT;
ALTER TABLE email.processed_emails ADD COLUMN IF NOT EXISTS received_at TIMESTAMPTZ;
