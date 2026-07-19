-- Deliberately NO retroactive analysis: emails stored before the detection pipeline
-- existed (recognizable by their missing body) stay out of the work queue. Classifying
-- stale mail would burn LLM budget on already-seen messages and could re-decide current
-- stages or auto-create applications from months-old threads.
UPDATE email.processed_emails
    SET analysis_status = 'irrelevant'
    WHERE analysis_status = 'pending' AND body IS NULL;

-- New terminal state for relevant-but-below-confidence-threshold verdicts, distinct from
-- genuinely irrelevant mail — makes threshold tuning observable and lets those rows be
-- re-queued later if thresholds change.
ALTER TABLE email.processed_emails
    DROP CONSTRAINT processed_emails_analysis_status_check;
ALTER TABLE email.processed_emails
    ADD CONSTRAINT processed_emails_analysis_status_check
    CHECK (analysis_status IN ('pending', 'analyzed', 'irrelevant', 'low_confidence', 'failed'));
