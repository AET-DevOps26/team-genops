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

## Kubernetes deployment

The Helm chart (`infra/helm/jobready/`) deploys email behind `email.enabled`. CD (Ansible
`deploy.yml`) flips that flag automatically based on whether the vault holds the Gmail
credentials (`vault_google_client_id/secret`, `vault_email_token_enc_key`,
`vault_state_signing_key`) and creates the `email-secrets` Secret from them. The gateway
routes `/api/v1/email/**` here (`EMAIL_URI`), with the OAuth callback public at the edge
(signed `state` is the trust anchor).

Per-host manual steps:
1. Add the four vault vars (`ansible-vault edit inventories/<env>/group_vars/vault.yml`).
2. Register `https://<ingress-host>/api/v1/email/connections/gmail/callback` as an authorized
   redirect URI in the Google Cloud console (the chart derives `GOOGLE_REDIRECT_URI` and
   `FRONTEND_REDIRECT_URL` from `ingress.host`).

**Never rotate `EMAIL_TOKEN_ENC_KEY` casually** — a new key orphans every stored Gmail token
and all users must reconnect their mailbox.

Smoke check after deploy: logged in, `GET https://<host>/api/v1/email/connections` returns
`{"connected": false}` (not a 404), and the Connect flow on the Profile page round-trips.

## Single-instance assumptions

`@Scheduled` polling + analysis and the in-process OAuth nonce store are per-process — running
more than one replica wastes Gmail quota and duplicates LLM calls (correctness is preserved by
the DB dedupe and idempotent internal APIs). A multi-replica fix is a Redis leader lock.

## Testing

```sh
./mvnw test                # full suite
./mvnw verify              # build + test + package — mirrors CI (Spotless + Checkstyle)
```

What's covered: web-layer security (`EmailControllerTest`), the Gmail OAuth connect flow and
signed single-use `state` tokens (`EmailConnectionServiceTest`, `StateTokenServiceTest`),
AES-GCM token encryption at rest (`TokenEncryptorTest`), Gmail body extraction
(`GmailBodyExtractionTest`), and the poller + LLM analysis pipeline (`EmailPollerTest`,
`EmailAnalysisServiceTest`) with the genai/application clients faked at the HTTP boundary.
