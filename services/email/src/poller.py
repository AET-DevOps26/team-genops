"""Background poller: periodically fetch and store new emails for every connection.

APScheduler runs `poll_once` on a fixed interval. For each connection it refreshes the
access token if expired, lists recent Gmail messages, and inserts any not already stored
(deduped on the `(user_id, message_id)` unique constraint).

NOTE: the scheduler runs per-process, so this is single-instance only. With >1 replica
each would poll independently (correctness is preserved by the dedupe, but Gmail quota is
wasted). A multi-replica fix is a Redis leader lock — out of scope here.
"""

from __future__ import annotations

import logging
from datetime import datetime, UTC

from apscheduler.schedulers.background import BackgroundScheduler
from sqlalchemy.orm import Session

from . import gmail_client
from .config import get_settings
from .db import (
    Connection,
    SessionLocal,
    insert_processed_email,
    list_connections,
    update_tokens,
)

logger = logging.getLogger(__name__)
_settings = get_settings()

_scheduler: BackgroundScheduler | None = None


def _fresh_access_token(db: Session, conn: Connection) -> str:
    """Return a valid access token, refreshing and persisting it if expired."""
    if conn.token_expiry > datetime.now(tz=UTC):
        return conn.access_token
    access_token, expiry = gmail_client.refresh_access_token(conn.refresh_token)
    update_tokens(db, user_id=conn.user_id, access_token=access_token, token_expiry=expiry)
    return access_token


def _poll_connection(db: Session, conn: Connection) -> int:
    """Fetch and store new messages for one connection. Returns count stored."""
    access_token = _fresh_access_token(db, conn)
    stored = 0
    for message_id in gmail_client.list_recent_message_ids(access_token, conn.refresh_token):
        msg = gmail_client.fetch_message(access_token, conn.refresh_token, message_id)
        inserted = insert_processed_email(
            db,
            user_id=conn.user_id,
            message_id=msg["message_id"],
            subject=msg.get("subject"),
            sender=msg.get("sender"),
            snippet=msg.get("snippet"),
            received_at=msg.get("received_at"),
        )
        if inserted:
            stored += 1
    return stored


def _poll_connection_isolated(conn: Connection) -> int:
    """Poll one connection in its own session so a failure can't affect the others.

    Each connection gets a dedicated session/transaction: a commit failure (constraint,
    serialization, lost connection) poisons only this session, which is then rolled back
    and discarded — the next connection starts from a clean one. Returns count stored, or
    0 if the connection failed.
    """
    db = SessionLocal()
    try:
        return _poll_connection(db, conn)
    except Exception:  # noqa: BLE001 — one bad connection must not stop the rest
        logger.exception("Polling failed for user %s", conn.user_id)
        db.rollback()
        return 0
    finally:
        db.close()


def poll_once() -> int:
    """Run one poll pass over all connections. Returns total messages stored."""
    listing = SessionLocal()
    try:
        connections = list_connections(listing)
    finally:
        listing.close()

    return sum(_poll_connection_isolated(conn) for conn in connections)


def _job() -> None:
    stored = poll_once()
    if stored:
        logger.info("Poller stored %d new email(s)", stored)


def start_poller() -> None:
    global _scheduler
    if _scheduler is not None:
        return
    _scheduler = BackgroundScheduler(timezone="UTC")
    _scheduler.add_job(
        _job,
        "interval",
        seconds=_settings.email_poll_interval_seconds,
        id="email-poll",
        max_instances=1,
        coalesce=True,
    )
    _scheduler.start()
    logger.info("Email poller started (interval=%ss)", _settings.email_poll_interval_seconds)


def stop_poller() -> None:
    global _scheduler
    if _scheduler is not None:
        _scheduler.shutdown(wait=False)
        _scheduler = None
