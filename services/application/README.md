# Application Service

Job application tracking: applications CRUD with stages, per-application recommendations,
and an append-only timeline of events. Public REST contract lives in `api/openapi.yaml`
(single source of truth — regenerate with `make -C api generate`).

## Internal API (machine-to-machine)

The `/internal/**` endpoints are **not** part of `api/openapi.yaml`: they are a trusted
backend-to-backend contract used by the email service's application-detection pipeline, with a
different auth model. They are guarded by a static shared secret (`INTERNAL_SERVICE_TOKEN`,
sent as `Authorization: Bearer <token>`; see `internal/InternalTokenFilter`). If the env var
is unset the internal API rejects everything (fail-closed).

Because the caller is a trusted service acting in the background (no live user request),
these endpoints are the **one sanctioned exception** to the "user_id from JWT only" rule —
the caller names the user explicitly. The token must never be exposed outside the cluster.

### `GET /internal/v1/users/{userId}/applications`

The user's applications, slimmed to what email matching needs:

```json
[{ "id": "…", "company": "Acme", "jobTitle": "Engineer", "stage": "applied", "updatedAt": "…" }]
```

### `POST /internal/v1/applications/{applicationId}/email-update`

Applies everything derived from one email atomically: optional stage change, one timeline
event, and zero or more recommendations. Idempotent on `sourceMessageId` (the Gmail message
id) — a replay returns `{"applied": false}` and changes nothing.

```json
{
  "userId": "…",
  "sourceMessageId": "gmail-message-id",
  "suggestedStage": "interview",
  "event": {
    "eventType": "interview_scheduled",
    "title": "Interview invitation",
    "description": "Acme invited you to interview",
    "occurredAt": "2026-07-14T10:00:00Z"
  },
  "recommendations": [
    { "insight": "Interview scheduled for Friday", "recommendedAction": "Prepare for the interview" }
  ]
}
```

Responses: `200 {"applied": true|false}`, `404` if the application doesn't belong to
`userId`, `422` on invalid enum values or missing fields, `401` on a missing/wrong token.

Event types: `stage_change`, `email_received`, `interview_scheduled`, `offer_received`,
`rejection`, `info_requested`, `note` (mirrors the public `ApplicationEventType` schema).

### `POST /internal/v1/users/{userId}/applications/email-create`

Auto-creates an application for a company the user does not track yet, derived from one
email (the pipeline only calls this at high classification confidence). `suggestedStage`
is `applied` (default) or `interview` for an interview invitation; `position` may be null.
Idempotent on `(userId, sourceMessageId)` — a replay returns `{"created": false}`. If an
application for `company` already exists (case-insensitive), the payload is applied to it
as an email-update instead and `{"created": false, "applicationId": "<existing>"}` is
returned.

```json
{
  "userId": "…",
  "sourceMessageId": "gmail-message-id",
  "company": "Acme",
  "position": "Software Engineer",
  "suggestedStage": "interview",
  "event": { "eventType": "interview_scheduled", "title": "…", "description": "…", "occurredAt": "…" },
  "recommendations": []
}
```

Responses: `200 {"created": true|false, "applicationId": "…"}`, `400` if the path and body
user ids differ, `422` on invalid enum values, `401` on a missing/wrong token.

## Timeline events

`GET /api/v1/applications/{id}/events` (public, JWT-authenticated) returns the timeline,
newest first by `occurred_at`. Events are appended by the email pipeline (source `email`)
and by manual stage changes through `PUT /api/v1/applications/{id}` (source `manual`).

## Testing

```sh
./mvnw test                # full suite
./mvnw verify              # build + test + package — mirrors CI (Spotless + Checkstyle)
```

What's covered: application CRUD + ownership rules (`ApplicationServiceImplTest`), web-layer
security for the public API (`ApplicationControllerTest`), the static-token internal API
(`InternalControllerTest`), email-driven updates + auto-create (`EmailUpdateServiceTest`), and
stage persistence mapping (`StageConverterTest`). Cross-service flows through the gateway are
covered by [`e2e_tests/`](../../e2e_tests/) (`test_application_flow.py`).
