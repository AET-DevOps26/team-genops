-- Application-detection pipeline state. Each stored email carries its (truncated) body and an
-- analysis lifecycle: pending → analyzed | irrelevant | failed (after bounded retries).
ALTER TABLE email.processed_emails
    ADD COLUMN body text,
    ADD COLUMN analysis_status varchar(20) NOT NULL DEFAULT 'pending'
        CHECK (analysis_status IN ('pending', 'analyzed', 'irrelevant', 'failed')),
    ADD COLUMN analysis_attempts int NOT NULL DEFAULT 0,
    ADD COLUMN matched_application_id uuid;

-- Covers the analyzer's work queue: only pending rows are ever scanned.
CREATE INDEX ix_processed_emails_analysis_pending
    ON email.processed_emails (processed_at)
    WHERE analysis_status = 'pending';
