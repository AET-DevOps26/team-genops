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
    job_description TEXT NOT NULL,
    job_url         VARCHAR(512),
    company_website VARCHAR(512),
    linkedin_url    VARCHAR(512),
    stage           VARCHAR(50) NOT NULL DEFAULT 'draft'
                        CHECK (stage IN ('draft', 'applied', 'follow_up', 'interview', 'offer', 'closed')),
    notes           TEXT,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application.recommendations (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL,
    application_id     UUID NOT NULL REFERENCES application.applications(id) ON DELETE CASCADE,
    insight            TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
