# Microservice Design — GenOps Job Tracker

## TL;DR

Five services derived from five bounded contexts. Four Spring Boot services share one Postgres instance with schema-per-service enforced by DB-user permissions. One Python/FastAPI GenAI service is stateless. Cover-letter generation is GenAI-orchestrated (read-only fan-out, client triggers save); email-insights generation is `application`-orchestrated (because it writes domain state). JWT is token-forwarded and verified at every hop. Mock-interview chat and Resume PDF parsing are descoped from v1.

---

## 1. Service Map

| Service | Language | Bounded Context | DB Schema | Stateless? |
|---|---|---|---|---|
| `auth` | Spring Boot | Identity | `auth` | yes (JWT only) |
| `application` | Spring Boot | Job Application Tracking + Recommendations | `application` | yes |
| `email` | Spring Boot | Email Integration (external adapter) | `email` | yes |
| `document` | Spring Boot | Document Storage (Profile, CoverLetter) | `document` | yes |
| `genai` | Python / FastAPI | GenAI Generation | *(none)* | yes |

Satisfies the project requirement of ≥3 Spring Boot services with margin. Each service owns exactly one bounded context with no overlapping responsibilities.

---

## 2. Bounded Contexts (the *why*)

1. **Identity** — credentials, registration, JWT issuance. Exists before any job-search activity. Kept lean: `(id, email, hashed_password)` only. Display name and professional details live in Profile, not here.
2. **Job Application Tracking** — the `JobApplication` aggregate (company, role, status, applied date, notes) plus `Recommendation` denormalized as state on each application. Recommendation does *not* warrant its own service: zero independent lifecycle, always consumed at the application card level.
3. **Email Integration** — pure external adapter for Gmail/Outlook. Owns OAuth tokens and synced email data. Knows nothing about applications or recommendations. This deliberate ignorance is the bounded context.
4. **Document Storage** — `Profile` (structured candidate data, the v1 primary) and `CoverLetter` (AI-generated, user-editable). `Resume` PDF parsing is deferred. PDF *export* of cover letters lives here too — a presentation concern of the document.
5. **GenAI Generation** — stateless LLM-backed capabilities: cover-letter drafting, email-insight extraction, role-fit analysis, insight drill-down chat. Read-only against all other services; never writes domain state.

---

## 3. Data Ownership Map

| Entity | Owning Service | Schema.Table | Notes |
|---|---|---|---|
| User | `auth` | `auth.users` | id, email, hashed_password only |
| Profile | `document` | `document.profiles` | structured candidate data; v1 primary |
| Resume | `document` | `document.resumes` | **deferred from v1** |
| CoverLetter | `document` | `document.cover_letters` | content + edits; PDF generated on demand |
| EmailConnection | `email` | `email.email_connections` | OAuth refresh tokens |
| Email | `email` | `email.emails` | normalized email data |
| JobApplication | `application` | `application.job_applications` | core domain object |
| Recommendation | `application` | `application.recommendations` | FK to job_application; denormalized insight |

`genai` has **no schema**. Insight-chat transcripts live in browser state for v1.

---

## 4. Communication Patterns (per flow)

### 4.1 Cover-letter generation — *GenAI orchestrates*

```
Client ──(JWT)──> genai-service POST /api/v1/cover-letter/generate
                       │ {applicationId? , target?}
                       ├──> document-service GET /api/v1/profile/me
                       └──> application-service GET /api/v1/applications/{id}   [if applicationId given]
                       │
                       └──> LLM
                       returns: { content }

Client ──(JWT)──> document-service POST /api/v1/cover-letters   [optional, user-initiated save]
Client ──(JWT)──> document-service GET /api/v1/cover-letters/{id}?format=pdf   [optional export]
```

GenAI fans out to *read* but never writes. Client decides whether to persist.

### 4.2 Email insights — *Application service orchestrates*

```
Client ──(JWT)──> application-service POST /api/v1/applications/refresh-insights
                       │
                       ├──> email-service GET /api/v1/emails/since?ts=...
                       ├──> genai-service POST /api/v1/email-insights
                       │      { emails, applications }
                       │      returns: [{applicationId, insight, recommendedAction}]
                       │
                       └──> writes Recommendation rows to application.recommendations
                       returns: updated applications
```

Application service is the conductor because it owns the destination table. Email service stays a dumb adapter; GenAI stays read-only. v1 trigger = on-demand button. Cron is a future stretch (and will need service-to-service auth to handle missing user JWT).

### 4.3 Insight drill-down chat — *stateless, browser-held*

```
Client (with full transcript) ──(JWT)──> genai-service POST /api/v1/insight-chat
                                              { recommendationId, messages: [...], userMessage }
                                              returns: { assistantMessage }
```

No `chatId`, no DB, no session. If persistence is added later, it goes in `application` next to Recommendation, *not* in `genai`.

### 4.4 Role-fit analysis — *GenAI orchestrates (same shape as cover letter)*

```
Client ──(JWT)──> genai-service POST /api/v1/role-fit
                       │
                       ├──> document-service GET /api/v1/profile/me
                       └──> application-service GET /api/v1/applications/{id}
                       │
                       └──> LLM
                       returns: { strengths[], gaps[], summary }
```

No persistence; client displays and discards. Same orchestrator-owns-destination rule applies — destination is the client.

---

## 5. Cross-cutting concerns

### 5.1 Database topology

- **One Postgres instance**, four schemas (`auth`, `application`, `email`, `document`).
- **Each service has its own DB user** with `GRANT` only on its own schema. Cross-schema queries fail at the database layer — boundary enforced by permissions, not convention.
- `genai` has no DB connection at all.

### 5.2 Authentication propagation

- Gateway (Traefik / NGINX) verifies JWT signature on ingress.
- **Every service re-verifies** the JWT using `auth`'s public key (mounted as a Kubernetes Secret). Defense in depth.
- Services **forward** the `Authorization: Bearer <jwt>` header on outgoing service-to-service calls.
- `user_id` is **always extracted from the JWT claim**. Never accepted in request body, query string, or any other header. This is non-negotiable.

### 5.3 OpenAPI-first

- Single source of truth: `api/openapi.yaml`.
- Generated clients only (Java for SB→SB, Python for SB→GenAI, TypeScript for client→all). No hand-written DTOs.
- Lint blocks merge.

### 5.4 Pact contract test pairs

| Consumer | Provider | Why |
|---|---|---|
| `web-client` | `auth` | login, register |
| `web-client` | `application` | applications CRUD, refresh-insights |
| `web-client` | `document` | profile, cover-letter save/export |
| `web-client` | `genai` | cover-letter generate, insight-chat, role-fit |
| `genai` | `document` | profile fetch (cover-letter, role-fit flows) |
| `genai` | `application` | application fetch (cover-letter, role-fit flows) |
| `application` | `email` | emails fetch (refresh-insights flow) |
| `application` | `genai` | email-insights call (refresh-insights flow) |

---

## 6. Scope decisions

**In v1:**
- Cover-letter generation + edit + save + PDF export
- Email integration (Gmail OAuth, fetch threads)
- Email insights (Recommendation generation, on-demand trigger)
- Role-fit analysis
- Insight drill-down chat (browser-state only)

**Deferred / out of v1:**
- Resume PDF upload + parsing (Profile is the primary candidate-context source)
- Mock-interview chat
- Background cron polling for insights
- Server-side chat persistence
- RAG with Weaviate (optional bonus per problem statement)

---

## 7. Non-obvious trade-offs (the design defense)

The choices below aren't self-evident from the diagram. Read this section before the oral exam.

1. **Asymmetric orchestration is principled.** GenAI conducts cover-letter; Application conducts email-insights. The rule: *whoever owns the destination of the data is the orchestrator.* Cover-letter's destination is the client (might not save) → GenAI conducts. Email-insights' destination is `application.recommendations` → Application conducts. This is consistent application of one rule, not inconsistency.

2. **GenAI is read-only against other services.** Even though it produces text, it never writes to other services' tables. State mutation is the orchestrator's job. This invariant prevents the AI service from becoming a back-door to every domain.

3. **Email-service stays ignorant.** It does not know about JobApplications. It does not know about Recommendations. Its sole responsibility is "talk to Gmail/Outlook on the user's behalf and expose normalized email data". This deliberate ignorance keeps the integration boundary clean and reusable.

4. **Recommendation lives in `application`, not its own service.** It has no independent lifecycle, always consumed at the application-card level, and a separate service would force a cross-service join on every dashboard render — the distributed-monolith anti-smell.

5. **Schema-per-service in one Postgres**, not five Postgres instances. Same isolation guarantee (DB-user permissions block cross-schema reads), much lower operational cost. Defensible: enforce ownership at the database, not by convention.

6. **JWT token-forwarding for v1**, not service-to-service identity. Lower setup cost; acceptable risk for a 3-person student project. Document the upgrade path to mTLS / service-JWTs when cron flows arrive.

7. **No chat persistence in v1.** Chat is enrichment over the persisted insight, not load-bearing data. Browser-held transcript is sufficient. Adding persistence later is additive (new `chats` table in `application`), not structural.
