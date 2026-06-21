# Auth Service — Database Schema

Managed by **Flyway**. Migrations run automatically on service startup.

## Schema: `auth`

### `auth.users`
Stores registered user credentials.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `email` | VARCHAR(255) | Unique, not null |
| `password_hash` | VARCHAR(255) | Bcrypt hashed password |
| `created_at` | TIMESTAMPTZ | Set on insert |

### `auth.refresh_tokens`
Stores JWT refresh tokens to enable token validation and revocation.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | References the user (no FK constraint — microservices rule) |
| `token` | VARCHAR(512) | Unique refresh token string |
| `expires_at` | TIMESTAMPTZ | Token expiry time |
| `created_at` | TIMESTAMPTZ | Set on insert |

## Why Two Tables

- `users` — stores identity and credentials
- `refresh_tokens` — stores long-lived tokens so they can be validated and revoked on logout. Without this table, stolen refresh tokens could be used indefinitely.

## Migration Files

| File | Description |
|---|---|
| `V1__create_auth_schema.sql` | Creates `auth` schema, `users` and `refresh_tokens` tables |

## NOTE: Flyway Execution Requirements

Placing migrations in this directory is not sufficient by itself. The following configuration is also required:

1. Add the `flyway-core` and `flyway-database-postgresql` dependencies to `services/auth/pom.xml`.
2. Enable Flyway and configure the `auth` schema in `application.properties`:

   ```properties
   spring.flyway.enabled=true
   spring.flyway.default-schema=auth
   spring.flyway.schemas=auth
   ```

3. Disable Hibernate schema creation and use it only to validate the migrated schema:

   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   spring.jpa.properties.hibernate.default_schema=auth
   ```

4. Ensure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
   `SPRING_DATASOURCE_PASSWORD` point to the auth service database. The configured
   database user must have permission to create and modify the `auth` schema.

On startup, Flyway creates its schema-history table, applies migrations that have
not run yet, and then Hibernate validates the resulting tables against the JPA
entities. Applied migration files must not be modified; create a new versioned
migration such as `V2__description.sql` for subsequent schema changes.

## NOTE: Required Changes to `User.java`

The current `User.java` entity has one mismatch with the schema that must be fixed:

**Missing schema qualifier** — add `schema = "auth"` to `@Table`:
```java
@Table(schema = "auth", name = "users")
```

Without this, Hibernate will look for `users` in the default `public` schema instead of `auth.users` and will fail on startup.
