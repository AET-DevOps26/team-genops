"""Database engine, ORM models, and connection-repository helpers.

The `email_connections` table holds OAuth secrets, so `access_token`/`refresh_token`
are encrypted at rest with pgcrypto: written as `armor(pgp_sym_encrypt(...))` (ASCII,
so the existing TEXT columns are untouched) and read back with `pgp_sym_decrypt(dearmor(...))`.
The encryption key comes from `EMAIL_TOKEN_ENC_KEY`. Plaintext tokens never touch disk.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime

from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from .config import get_settings

_settings = get_settings()

engine = create_engine(_settings.db_url, pool_pre_ping=True, future=True)
SessionLocal = sessionmaker(bind=engine, expire_on_commit=False, future=True)


class Base(DeclarativeBase):
    pass


def get_db():
    """FastAPI dependency yielding a session that is always closed."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@dataclass
class Connection:
    """A decrypted view of a row in email.email_connections."""

    user_id: str
    provider: str
    email_address: str
    access_token: str
    refresh_token: str
    token_expiry: datetime
    created_at: datetime


def upsert_connection(
    db: Session,
    *,
    user_id: str,
    provider: str,
    email_address: str,
    access_token: str,
    refresh_token: str,
    token_expiry: datetime,
) -> None:
    """Insert or update the single connection for a user (user_id is UNIQUE).

    `updated_at` is set explicitly because there is no DB trigger.
    """
    db.execute(
        text(
            """
            INSERT INTO email.email_connections
                (user_id, provider, email_address, access_token, refresh_token, token_expiry)
            VALUES
                (:user_id, :provider, :email_address,
                 armor(pgp_sym_encrypt(:access_token, :enc_key)),
                 armor(pgp_sym_encrypt(:refresh_token, :enc_key)),
                 :token_expiry)
            ON CONFLICT (user_id) DO UPDATE SET
                provider      = EXCLUDED.provider,
                email_address = EXCLUDED.email_address,
                access_token  = EXCLUDED.access_token,
                refresh_token = EXCLUDED.refresh_token,
                token_expiry  = EXCLUDED.token_expiry,
                updated_at    = NOW()
            """
        ),
        {
            "user_id": user_id,
            "provider": provider,
            "email_address": email_address,
            "access_token": access_token,
            "refresh_token": refresh_token,
            "token_expiry": token_expiry,
            "enc_key": _settings.email_token_enc_key,
        },
    )
    db.commit()


def update_tokens(
    db: Session,
    *,
    user_id: str,
    access_token: str,
    token_expiry: datetime,
    refresh_token: str | None = None,
) -> None:
    """Persist a refreshed access token (and optionally a rotated refresh token)."""
    if refresh_token is None:
        db.execute(
            text(
                """
                UPDATE email.email_connections SET
                    access_token = armor(pgp_sym_encrypt(:access_token, :enc_key)),
                    token_expiry = :token_expiry,
                    updated_at   = NOW()
                WHERE user_id = :user_id
                """
            ),
            {
                "user_id": user_id,
                "access_token": access_token,
                "token_expiry": token_expiry,
                "enc_key": _settings.email_token_enc_key,
            },
        )
    else:
        db.execute(
            text(
                """
                UPDATE email.email_connections SET
                    access_token  = armor(pgp_sym_encrypt(:access_token, :enc_key)),
                    refresh_token = armor(pgp_sym_encrypt(:refresh_token, :enc_key)),
                    token_expiry  = :token_expiry,
                    updated_at    = NOW()
                WHERE user_id = :user_id
                """
            ),
            {
                "user_id": user_id,
                "access_token": access_token,
                "refresh_token": refresh_token,
                "token_expiry": token_expiry,
                "enc_key": _settings.email_token_enc_key,
            },
        )
    db.commit()


def get_connection(db: Session, user_id: str) -> Connection | None:
    """Fetch and decrypt a user's connection, or None if not connected."""
    row = (
        db.execute(
            text(
                """
            SELECT user_id, provider, email_address,
                   pgp_sym_decrypt(dearmor(access_token), :enc_key)  AS access_token,
                   pgp_sym_decrypt(dearmor(refresh_token), :enc_key) AS refresh_token,
                   token_expiry, created_at
            FROM email.email_connections
            WHERE user_id = :user_id
            """
            ),
            {"user_id": user_id, "enc_key": _settings.email_token_enc_key},
        )
        .mappings()
        .first()
    )
    if row is None:
        return None
    return Connection(
        user_id=str(row["user_id"]),
        provider=row["provider"],
        email_address=row["email_address"],
        access_token=row["access_token"],
        refresh_token=row["refresh_token"],
        token_expiry=row["token_expiry"],
        created_at=row["created_at"],
    )


def list_connections(db: Session) -> list[Connection]:
    """All connections — used by the poller to iterate every connected user."""
    rows = (
        db.execute(
            text(
                """
            SELECT user_id, provider, email_address,
                   pgp_sym_decrypt(dearmor(access_token), :enc_key)  AS access_token,
                   pgp_sym_decrypt(dearmor(refresh_token), :enc_key) AS refresh_token,
                   token_expiry, created_at
            FROM email.email_connections
            """
            ),
            {"enc_key": _settings.email_token_enc_key},
        )
        .mappings()
        .all()
    )
    return [
        Connection(
            user_id=str(r["user_id"]),
            provider=r["provider"],
            email_address=r["email_address"],
            access_token=r["access_token"],
            refresh_token=r["refresh_token"],
            token_expiry=r["token_expiry"],
            created_at=r["created_at"],
        )
        for r in rows
    ]


def delete_connection(db: Session, user_id: str) -> None:
    db.execute(
        text("DELETE FROM email.email_connections WHERE user_id = :user_id"),
        {"user_id": user_id},
    )
    db.commit()


def insert_processed_email(
    db: Session,
    *,
    user_id: str,
    message_id: str,
    subject: str | None,
    sender: str | None,
    snippet: str | None,
    received_at: datetime | None,
) -> bool:
    """Insert a fetched email, deduped on (user_id, message_id).

    Returns True if a new row was inserted, False if it already existed.
    """
    result = db.execute(
        text(
            """
            INSERT INTO email.processed_emails
                (user_id, message_id, subject, sender, snippet, received_at)
            VALUES
                (:user_id, :message_id, :subject, :sender, :snippet, :received_at)
            ON CONFLICT (user_id, message_id) DO NOTHING
            """
        ),
        {
            "user_id": user_id,
            "message_id": message_id,
            "subject": subject,
            "sender": sender,
            "snippet": snippet,
            "received_at": received_at,
        },
    )
    db.commit()
    return result.rowcount > 0  # type: ignore[attr-defined]  # DML returns CursorResult


def list_processed_emails(db: Session, user_id: str, *, limit: int, offset: int) -> list[dict]:
    rows = (
        db.execute(
            text(
                """
            SELECT message_id, subject, sender, snippet, received_at
            FROM email.processed_emails
            WHERE user_id = :user_id
            ORDER BY received_at DESC NULLS LAST, processed_at DESC
            LIMIT :limit OFFSET :offset
            """
            ),
            {"user_id": user_id, "limit": limit, "offset": offset},
        )
        .mappings()
        .all()
    )
    return [dict(r) for r in rows]
