# Application Service — Database Schema

Managed by **Flyway**. Migrations run automatically on service startup.

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
| `job_url` | VARCHAR(512) | Optional |
| `stage` | VARCHAR(50) | `applied`, `follow_up`, `interview`, `offer`, `closed` |
| `notes` | TEXT | Optional user notes |
| `applied_at` | TIMESTAMPTZ | Set on insert |
| `updated_at` | TIMESTAMPTZ | Updated on stage change |

### `application.fit_analyses`
Stores AI-generated fit analysis results per application.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Owner |
| `application_id` | UUID | References `applications.id` (no FK — microservices rule) |
| `strengths` | TEXT | AI-identified strengths for this role |
| `gaps` | TEXT | AI-identified gaps for this role |
| `summary` | TEXT | Overall fit summary |
| `created_at` | TIMESTAMPTZ | Set on insert |

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
- `fit_analyses.application_id` references `applications.id` within the same schema — no cross-schema FK constraints
- `user_id` is never accepted from the request body — always extracted from the JWT `sub` claim

## Migration Files

| File | Description |
|---|---|
| `V1__create_application_schema.sql` | Creates `application` schema, `applications` and `fit_analyses` tables |

## NOTE: Flyway Execution Requirements

Placing migrations in this directory is not sufficient by itself. The following configuration is required from the service owner:

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

4. All JPA entities must use `@Table(schema = "application", name = "table_name")`.
