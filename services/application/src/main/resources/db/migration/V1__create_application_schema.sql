-- ========================
-- SCHEMA: application
-- ========================
CREATE SCHEMA IF NOT EXISTS application;

CREATE TABLE application.applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    company         VARCHAR(255) NOT NULL,
    job_title       VARCHAR(255) NOT NULL,
    job_description TEXT,
    job_url         VARCHAR(512),
    stage           VARCHAR(50) NOT NULL DEFAULT 'applied'
                        CHECK (stage IN ('applied', 'follow_up', 'interview', 'offer', 'closed')),
    notes           TEXT,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE application.fit_analyses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    application_id UUID NOT NULL,
    strengths      TEXT NOT NULL,
    gaps           TEXT NOT NULL,
    summary        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
