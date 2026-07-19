# Email Service

Gmail integration (Spring Boot): OAuth2 connect/disconnect, a `@Scheduled` background poller
that stores new messages, and the **application-detection pipeline** that turns job-related
emails into application updates. Public REST contract lives in `api/openapi.yaml`.

## Application-detection pipeline

Each poll cycle (`email.poll-interval-seconds`, default 300s):

1. New Gmail messages are fetched in full (`format=full`) — the plain-text body is extracted
   (HTML fallback with tags stripped), truncated to 8000 chars, and stored with
   `analysis_status = pending`.
2. `EmailAnalysisService.analyzePending()` processes up to `email.analysis.batch-size` pending
   emails:
   - A **keyword pre-filter** on the subject skips obvious bulk mail (newsletter, unsubscribe,
     job alerts, …) without an LLM call — unless a clearly job-related keyword (interview,
     offer, application, …) is also present. Everything else goes to the LLM; the filter is a
     cost optimization, never the relevance decision.
   - genai's `POST /internal/v1/email-analysis` classifies the email against the user's
     applications (fetched from the application service's internal API). Relevance is judged
     from **subject and body content** — a sender address that doesn't match the company is
     explicitly not disqualifying (recruiters, ATS domains, forwarding).
   - A confident verdict is applied via the application service:
     - matched candidate + confidence ≥ `email.analysis.confidence-threshold` (0.6) →
       `email-update` (stage change + timeline event + action items, idempotent on the Gmail
       message id);
     - no match, company extracted, confidence ≥ `email.analysis.create-confidence-threshold`
       (0.8) → `email-create` auto-creates the application (stage `interview` for interview
       invites, else `applied`).
3. Transient failures (genai or application service down) leave the email `pending` and count
   an attempt; after `email.analysis.max-attempts` (3) it is marked `failed`. Replays are safe
   — the application service dedupes on the message id. Failures never break polling.

The whole pipeline is **disabled while `INTERNAL_SERVICE_TOKEN` is blank** (and the internal
APIs it calls reject everything without the token — fail-closed). Configuration:
`GENAI_URL`, `APPLICATION_SERVICE_URL`, `INTERNAL_SERVICE_TOKEN` (see `.env.example`).

## Single-instance assumptions

`@Scheduled` polling + analysis and the in-process OAuth nonce store are per-process — running
more than one replica wastes Gmail quota and duplicates LLM calls (correctness is preserved by
the DB dedupe and idempotent internal APIs). A multi-replica fix is a Redis leader lock.
