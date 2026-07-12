# Application Service — Database Schema

> **Reference only — no Flyway.** This service creates its live schema from the `@Entity`
> classes via Hibernate `ddl-auto=update` (+ `create_namespaces=true`). The SQL here is
> unexecuted documentation kept in sync with the entities by hand. If the service ever moves
> to Flyway, see the note at the bottom.

## Schema: `application`

### `application.applications`
Tracks every job application and its current stage.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Owner — from JWT `sub` claim only |
| `company` | VARCHAR(255) | Not null |
| `job_title` | VARCHAR(255) | Not null |
| `job_description` | TEXT | Pasted by user — read by genai for generation |
| `job_url` | VARCHAR(512) | Optional — link to the job posting |
| `company_website` | VARCHAR(512) | Optional |
| `linkedin_url` | VARCHAR(512) | Optional |
| `stage` | VARCHAR(50) | `applied`, `follow_up`, `interview`, `offer`, `closed` |
| `notes` | TEXT | Optional user notes |
| `applied_at` | TIMESTAMPTZ | Set on insert |
| `updated_at` | TIMESTAMPTZ | Updated on stage change |

### `application.recommendations`
Stored next-best-action items per application (persistence only — this service never
generates them itself).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Owner |
| `application_id` | UUID | Parent application (same schema) |
| `insight` | TEXT | The observation the recommendation is based on |
| `recommended_action` | TEXT | The suggested next best action |
| `created_at` | TIMESTAMPTZ | Set on insert |

> ddl-auto does not create the FK/cascade shown in the reference SQL; the service layer
> deletes an application's recommendations explicitly when the application is deleted.

## Stage Lifecycle

```
applied → follow_up → interview → offer → closed
```

| Stage | Description |
|---|---|
| `applied` | Application submitted, awaiting response |
| `follow_up` | No response received — follow-up recommended |
| `interview` | Interview scheduled or in progress |
| `offer` | Offer received |
| `closed` | Position filled or application withdrawn |

## Key Relationships

- `job_description` is read by the **genai service** via REST to generate cover letters, resumes, and fit analyses
- `user_id` is never accepted from the request body — always extracted from the JWT `sub` claim

## Migration Files

| File | Description |
|---|---|
| `V1__create_application_schema.sql` | Reference SQL: `application` schema, `applications` and `recommendations` tables |

## NOTE: If Moving to Flyway Later

Placing migrations in this directory is not sufficient by itself. The following configuration
would be required:

1. Add the `flyway-core` and `flyway-database-postgresql` dependencies to `pom.xml`.
2. Enable Flyway and configure the `application` schema in `application.properties`:

   ```properties
   spring.flyway.enabled=true
   spring.flyway.default-schema=application
   spring.flyway.schemas=application
   ```

3. Disable Hibernate schema creation and use it only to validate the migrated schema:

   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.properties.hibernate.default_schema=application
   ```
