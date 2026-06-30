"""Version-tracked raw-SQL migration runner.

The repo keeps migrations as plain `.sql` files under `alembic/versions/` (no Alembic
infra). On startup we ensure the `email` schema and a `schema_migrations` ledger exist,
then apply every `*.sql` file not yet recorded, in filename order, each in its own
transaction. Re-runs are idempotent: already-applied files are skipped.
"""
from __future__ import annotations

import logging
from pathlib import Path

from sqlalchemy import Engine, text

logger = logging.getLogger(__name__)

VERSIONS_DIR = Path(__file__).resolve().parent.parent / "alembic" / "versions"


def _applied_versions(conn) -> set[str]:
    rows = conn.execute(text("SELECT version FROM email.schema_migrations")).scalars().all()
    return set(rows)


def run_migrations(engine: Engine, versions_dir: Path = VERSIONS_DIR) -> list[str]:
    """Apply pending .sql migrations. Returns the list of versions applied this run."""
    with engine.begin() as conn:
        conn.execute(text("CREATE SCHEMA IF NOT EXISTS email"))
        conn.execute(
            text(
                """
                CREATE TABLE IF NOT EXISTS email.schema_migrations (
                    version    TEXT PRIMARY KEY,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """
            )
        )

    sql_files = sorted(p for p in versions_dir.glob("*.sql"))
    applied: list[str] = []

    for path in sql_files:
        version = path.name
        with engine.begin() as conn:
            if version in _applied_versions(conn):
                continue
            logger.info("Applying migration %s", version)
            conn.execute(text(path.read_text()))
            conn.execute(
                text("INSERT INTO email.schema_migrations (version) VALUES (:v)"),
                {"v": version},
            )
            applied.append(version)

    if applied:
        logger.info("Applied %d migration(s): %s", len(applied), ", ".join(applied))
    else:
        logger.info("No pending migrations")
    return applied
