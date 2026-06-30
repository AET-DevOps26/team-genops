# Email Service — Database Schema

> **NOTE: This service is implemented in Python/FastAPI — NOT Spring Boot.**
> Python was chosen because OAuth2 integration with Gmail and Outlook is significantly
> easier with Python libraries (`google-auth-oauthlib`, `msal`) than in Java.

Migrations are plain **`.sql` files** in this directory, applied on service startup by a small
version-tracked runner (`src/migrate.py`) — the repo standardised on raw SQL rather than Alembic
Python revisions. On startup the runner ensures the `email` schema and an `email.schema_migrations`
ledger exist, then applies every `*.sql` file not yet recorded, in filename order, each in its own
transaction. Re-runs are idempotent: already-applied files are skipped. To add a migration, drop a new
`NNN_description.sql` file here — no further wiring needed.

## Schema: `email`

### `email.email_connections`
Stores OAuth2 credentials for a user's connected email account.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Unique — one connection per user |
| `provider` | VARCHAR(50) | `gmail` or `outlook` |
| `email_address` | VARCHAR(255) | The connected email address |
| `access_token` | TEXT | OAuth2 access token — short-lived |
| `refresh_token` | TEXT | OAuth2 refresh token — used to renew access token |
| `token_expiry` | TIMESTAMPTZ | Expiry time of the current access token |
| `created_at` | TIMESTAMPTZ | Set on insert |
| `updated_at` | TIMESTAMPTZ | Updated on token refresh |

## Key Notes

- One email connection per user (`user_id` is UNIQUE)
- `access_token` and `refresh_token` are sensitive — ensure the database user has restricted access to this schema
- `token_expiry` should be checked before every email fetch — refresh the access token if expired using `refresh_token`
- `user_id` is never accepted from the request body — always extracted from the JWT `sub` claim

### `email.processed_emails`
Tracks which emails have already been processed to prevent duplicate application detection.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `user_id` | UUID | Owner |
| `message_id` | VARCHAR(255) | Gmail/Outlook unique message ID |
| `processed_at` | TIMESTAMPTZ | Set on insert |

`(user_id, message_id)` is UNIQUE — guarantees no email is processed twice per user.

## How It Connects to Other Services

- The **email service** fetches emails from Gmail/Outlook using the stored OAuth2 tokens
- Detected job applications are passed to the **application service** to create/update records
- Email content is passed to the **genai service** via the application service for insight extraction

## Migration Files

| File | Description |
|---|---|
| `001_create_email_schema.sql` | Creates `email` schema, `email_connections`, and `processed_emails` |
| `002_extend_processed_emails.sql` | Adds fetched-content columns (`subject`, `sender`, `snippet`, `received_at`) |

## Token encryption at rest

`access_token` and `refresh_token` in `email_connections` are OAuth secrets. They are stored
encrypted using `pgcrypto` — written as `armor(pgp_sym_encrypt(token, :key))` and read back with
`pgp_sym_decrypt(dearmor(col), :key)`, keyed by `EMAIL_TOKEN_ENC_KEY`. Plaintext tokens never hit
disk. See `src/db.py`.
