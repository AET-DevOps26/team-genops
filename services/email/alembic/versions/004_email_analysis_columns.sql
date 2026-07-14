-- Application-detection pipeline: store the email body and track per-email analysis
-- state so failed LLM/service calls can be retried on later poll cycles.
--   pending    → not yet analyzed (or transient failure, retried next cycle)
--   analyzed   → matched an application and the update was applied
--   irrelevant → not about one of the user's applications (or no applications / low confidence)
--   failed     → gave up after email_analysis_max_attempts
ALTER TABLE email.processed_emails
    ADD COLUMN IF NOT EXISTS body TEXT,
    ADD COLUMN IF NOT EXISTS analysis_status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (analysis_status IN ('pending', 'analyzed', 'irrelevant', 'failed')),
    ADD COLUMN IF NOT EXISTS matched_application_id UUID,
    ADD COLUMN IF NOT EXISTS analysis_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS analyzed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_processed_emails_analysis_pending
    ON email.processed_emails (analysis_status)
    WHERE analysis_status = 'pending';
