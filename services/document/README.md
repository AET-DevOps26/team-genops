# Document Service

> JobReady's candidate-data store — owns the structured professional profile (work experience,
> education, skills, languages) and the generated documents (cover letters, resumes) produced
> by the GenAI service.

**Stack:** Spring Boot · Spring Data JPA · PostgreSQL (Flyway, `document` schema) ·
Spring Security OAuth2 Resource Server

---

## What it does

| Capability | Detail |
|---|---|
| **Profile aggregate** | One profile per user (name, bio, location, contact) plus sub-resources: work experiences, educations, skills (levelled), languages (proficiency-levelled) |
| **Generated documents** | Stores cover letters and resumes produced by `genai`, optionally filed under a job application — a document without an application is a first-class case ("tighten up my general resume") |
| **Context provider** | The GenAI service reads the profile through this API to ground its prompts (tailored cover letters, mock-interview preconditions) |

## API

All routes require a Bearer JWT — verified locally against the auth service's published JWK Set
(this service never signs tokens). Ownership always derives from the token's `sub` claim; a
`user_id` is never accepted from the request. DTOs are generated from
[`api/openapi.yaml`](../../api/openapi.yaml) (the controller implements the generated
`DocumentsApi` interface — no hand-written request/response classes).

### Profile — `/api/v1/profile`

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/profile` | Fetch the full profile aggregate (profile + all sub-resources) |
| `PUT` | `/profile` | Create or update the profile core |
| `POST` | `/profile/work-experiences` | Add a work experience |
| `PUT` / `DELETE` | `/profile/work-experiences/{id}` | Update / remove one |
| `POST` | `/profile/educations` | Add an education entry |
| `PUT` / `DELETE` | `/profile/educations/{id}` | Update / remove one |
| `POST` | `/profile/skills` | Add a skill (`beginner` → `expert`) |
| `PUT` / `DELETE` | `/profile/skills/{id}` | Update / remove one |
| `POST` | `/profile/languages` | Add a language (+ proficiency) |
| `PUT` / `DELETE` | `/profile/languages/{id}` | Update / remove one |

### Generated documents — `/api/v1/documents`

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/documents` | List the user's generated documents (filterable by application) |
| `POST` | `/documents` | Store a generated document (cover letter / resume) |
| `DELETE` | `/documents/{id}` | Delete a generated document |

Errors follow the spec's unified `{ code, message }` schema (`GlobalExceptionHandler`).

Interactive docs at `/swagger-ui.html` · OpenAPI JSON at `/v3/api-docs` (both public).

## Database

The service owns the **`document` schema** inside the shared Postgres instance. **Flyway owns
the schema** (`src/main/resources/db/migration/`); Hibernate only validates it
(`ddl-auto=validate`) — entity drift fails fast at startup instead of silently mutating the DB.

```
document.profiles            1 per user (user_id UNIQUE)
document.work_experiences ─┐
document.educations        │  sub-resources — FK → profiles(user_id),
document.skills            │  ON DELETE CASCADE
document.certifications    │
document.languages        ─┘
document.cover_letters       generated documents — application_id nullable (V2)
document.resumes             since a document need not target an application
```

Adding a migration: drop a `V<n>__description.sql` file in `db/migration/` — Flyway applies it
on next startup. Never edit an applied migration.

## Security model

- **JWT verification only** — RS256 signature checked against auth's JWKS
  (`AUTH_JWKS_URL`), fetched lazily and cached. No private keys in this service.
- **Stateless** — no sessions, CSRF disabled (bearer-token auth, not ambient cookies).
- **Public surface** — only health probes, `/actuator/prometheus`, and the Swagger/OpenAPI
  docs; everything else requires a valid token.
- **Defense in depth** — the gateway already validated the token at the edge; this service
  re-verifies it independently.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | — (required) | JDBC URL to the shared Postgres |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | — (required) | DB credentials |
| `AUTH_JWKS_URL` | `http://localhost:8080/...` (compose sets `http://auth:8080/...`) | Where to fetch auth's public keys |
| `APP_VERSION` | `dev` | Image tag injected by Helm — exported as `app_info{version}` |

**Ports.** The container listens on **8080**; Docker Compose remaps it to the documented
**8083** on the host. Health + metrics live on the separate **management port 8090**
(`/actuator/health`, `/actuator/prometheus`) — never exposed through the ingress.

## Running

### Via Docker Compose (recommended — full stack)

From the repo root:

```bash
docker compose up --build document
```

### Standalone (needs Postgres on localhost:5432)

```bash
cd services/document
# fill in src/main/resources/application-local.properties first
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # serves on 8083
```

> Requires **JDK 21** (`JAVA_HOME`) — palantir-java-format crashes on newer JDKs.

## Testing & Quality

```bash
./mvnw test                # unit + web-layer tests (service logic, controller security)
./mvnw verify              # build + test + package — mirrors CI (Spotless + Checkstyle)
./mvnw spotless:apply      # auto-fix formatting
```

## Source map

```
src/main/java/com/jobready/document/
├── DocumentServiceApplication.java
├── controller/ProfileController.java   # Implements the generated DocumentsApi interface
├── service/                            # DocumentService + impl — business logic, ownership checks
├── repository/                         # Spring Data JPA repositories (one per aggregate)
├── modelEntity/                        # JPA entities + enum converters (skill level, proficiency)
├── exception/                          # Not-found exceptions + unified {code,message} handler
├── config/
│   ├── SecurityConfig.java             # Public surface + JWT resource server
│   ├── JwtConfig.java                  # JWKS decoder
│   └── AppInfoConfig.java              # app_info{version} gauge
└── generated/                          # OpenAPI-generated API interface + DTOs (do not edit)

src/main/resources/db/migration/        # Flyway migrations (V1 schema, V2 nullable application_id)
```
