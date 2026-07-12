-- Single source of truth for the per-service Postgres databases.
--
-- Database-per-service: each service owns exactly one database and never touches
-- another's. This file is the one place those databases are declared. It runs
-- ONCE, automatically, the first time the postgres data volume is initialised
-- (mounted into /docker-entrypoint-initdb.d). To re-run it, drop the volume:
--   docker compose down -v

CREATE DATABASE auth_db;

CREATE DATABASE email_db;

-- application and document use schema-per-service isolation inside the shared
-- POSTGRES_DB (schemas `application` and `document` respectively), so they need
-- no dedicated database here. See CLAUDE.md > Architecture.
