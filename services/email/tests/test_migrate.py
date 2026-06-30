"""Migration runner tests.

These exercise the real Postgres-targeted SQL (pgcrypto, schemas), so they need a
database. They run when EMAIL_TEST_DB_URL points at a disposable Postgres and skip
otherwise, keeping the default CI (no DB service) green.
"""
import os

import pytest
from sqlalchemy import create_engine, text

from src.migrate import run_migrations

DB_URL = os.environ.get("EMAIL_TEST_DB_URL")
pytestmark = pytest.mark.skipif(not DB_URL, reason="EMAIL_TEST_DB_URL not set")


@pytest.fixture
def engine():
    eng = create_engine(DB_URL, future=True)
    with eng.begin() as conn:
        conn.execute(text("DROP SCHEMA IF EXISTS email CASCADE"))
    yield eng
    with eng.begin() as conn:
        conn.execute(text("DROP SCHEMA IF EXISTS email CASCADE"))


def test_migrations_apply_then_are_idempotent(engine):
    applied = run_migrations(engine)
    assert "001_create_email_schema.sql" in applied
    assert "002_extend_processed_emails.sql" in applied

    # Tables exist with the extended columns.
    with engine.begin() as conn:
        cols = conn.execute(
            text(
                """
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'email' AND table_name = 'processed_emails'
                """
            )
        ).scalars().all()
    assert {"subject", "sender", "snippet", "received_at"} <= set(cols)

    # Re-running applies nothing.
    assert run_migrations(engine) == []
