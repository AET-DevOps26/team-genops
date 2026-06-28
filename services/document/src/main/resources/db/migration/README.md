# Document Service — Database Schema

Managed by **Flyway**. Migrations run automatically on service startup.

## Schema: `document`

### `document.profiles`
One profile per user — the single source of truth for all AI-generated outputs.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Unique — one profile per user |
| `first_name` | VARCHAR(100) | Not null |
| `last_name` | VARCHAR(100) | Not null |
| `bio` | TEXT | Optional summary |
| `location` | VARCHAR(255) | Optional |
| `phone` | VARCHAR(50) | Optional |
| `website` | VARCHAR(255) | Optional |
| `created_at` | TIMESTAMPTZ | Set on insert |
| `updated_at` | TIMESTAMPTZ | Updated on change |

### `document.work_experiences`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `company` | VARCHAR(255) | Not null |
| `role` | VARCHAR(255) | Not null |
| `location` | VARCHAR(255) | Optional |
| `start_date` | DATE | Not null |
| `end_date` | DATE | Null if current |
| `is_current` | BOOLEAN | Default false |
| `description` | TEXT | Optional |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.educations`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `institution` | VARCHAR(255) | Not null |
| `degree` | VARCHAR(255) | Not null |
| `field` | VARCHAR(255) | Optional |
| `start_date` | DATE | Not null |
| `end_date` | DATE | Optional |
| `description` | TEXT | Optional |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.skills`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `name` | VARCHAR(100) | Not null |
| `level` | VARCHAR(50) | `beginner`, `intermediate`, `advanced`, `expert` |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.certifications`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `name` | VARCHAR(255) | Not null |
| `issuer` | VARCHAR(255) | Optional |
| `issued_date` | DATE | Optional |
| `expiry_date` | DATE | Optional |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.languages`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `name` | VARCHAR(100) | Not null |
| `proficiency` | VARCHAR(50) | `basic`, `conversational`, `fluent`, `native` |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.cover_letters`
Stores AI-generated cover letters per application.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `application_id` | UUID | References application service (no FK — microservices rule) |
| `content` | TEXT | Full cover letter text |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `document.resumes`
Stores AI-generated resumes per application.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `user_id` | UUID | Owner |
| `application_id` | UUID | References application service (no FK — microservices rule) |
| `content` | TEXT | Full resume text |
| `created_at` | TIMESTAMPTZ | Set on insert |

## Migration Files

| File | Description |
|---|---|
| `V1__create_document_schema.sql` | Creates `document` schema and all tables |

## NOTE: Flyway Execution Requirements

Placing migrations in this directory is not sufficient by itself. The following configuration is required from the service owner:

1. Add the `flyway-core` and `flyway-database-postgresql` dependencies to `pom.xml`.
2. Enable Flyway and configure the `document` schema in `application.properties`:

   ```properties
   spring.flyway.enabled=true
   spring.flyway.default-schema=document
   spring.flyway.schemas=document
   ```

3. Disable Hibernate schema creation and use it only to validate the migrated schema:

   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.properties.hibernate.default_schema=document
   ```

4. All JPA entities must use `@Table(schema = "document", name = "table_name")`.
