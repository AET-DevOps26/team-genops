-- Single source of truth for the per-service Postgres databases.
--
-- Database-per-service: each service owns exactly one database and never touches
-- another's. This file is the one place those databases are declared. It runs
-- ONCE, automatically, the first time the postgres data volume is initialised
-- (mounted into /docker-entrypoint-initdb.d). To re-run it, drop the volume:
--   docker compose down -v

CREATE DATABASE auth_db;

CREATE DATABASE email_db;

CREATE DATABASE application_db;

-- Future services — uncomment when their service is added:
-- CREATE DATABASE document_db;
