-- ========================
-- SCHEMA: application
-- ========================
-- NOTE: unexecuted reference SQL. The live schema comes from Hibernate ddl-auto=update
-- (this service has no Flyway); keep this file in sync with the @Entity classes by hand.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS application;

CREATE TABLE application.applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    company         VARCHAR(255) NOT NULL,
    job_title       VARCHAR(255) NOT NULL,
    job_description TEXT,
    job_url         VARCHAR(512),
    company_website VARCHAR(512),
    linkedin_url    VARCHAR(512),
    stage           VARCHAR(50) NOT NULL DEFAULT 'applied'
                        CHECK (stage IN ('applied', 'follow_up', 'interview', 'offer', 'closed')),
    notes           TEXT,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Append-only timeline of an application: detected emails, stage changes, interviews, etc.
-- occurred_at is when the event actually happened (email received date); created_at is the
-- row's write time. source_message_id (Gmail id) dedupes email-derived events on retries.
CREATE TABLE application.application_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    application_id    UUID NOT NULL REFERENCES application.applications(id) ON DELETE CASCADE,
    event_type        VARCHAR(50) NOT NULL
                          CHECK (event_type IN ('stage_change', 'email_received', 'interview_scheduled',
                                                'offer_received', 'rejection', 'info_requested', 'note')),
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    stage_from        VARCHAR(50),
    stage_to          VARCHAR(50),
    source            VARCHAR(20) NOT NULL CHECK (source IN ('EMAIL', 'MANUAL')),
    source_message_id VARCHAR(255),
    occurred_at       TIMESTAMPTZ NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application.recommendations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    application_id     UUID NOT NULL REFERENCES application.applications(id) ON DELETE CASCADE,
    insight            TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
