"""Poller tests — focus on dedupe across repeated polls (Gmail + DB mocked)."""
from datetime import datetime, timedelta, timezone

from src import poller
from src.db import Connection


class FakeSession:
    """Minimal stand-in for a SQLAlchemy Session (writes are mocked out)."""

    def __init__(self) -> None:
        self.rollbacks = 0

    def rollback(self) -> None:
        self.rollbacks += 1

    def close(self) -> None:
        pass


def _use_fake_sessions(monkeypatch) -> list[FakeSession]:
    """Patch poller.SessionLocal to hand out tracked FakeSessions. Returns the list."""
    created: list[FakeSession] = []

    def factory() -> FakeSession:
        session = FakeSession()
        created.append(session)
        return session

    monkeypatch.setattr(poller, "SessionLocal", factory)
    return created


def _connection() -> Connection:
    return Connection(
        user_id="user-1",
        provider="gmail",
        email_address="jane@gmail.com",
        access_token="at",
        refresh_token="rt",
        token_expiry=datetime.now(tz=timezone.utc) + timedelta(hours=1),  # not expired
        created_at=datetime.now(tz=timezone.utc),
    )


def test_second_poll_stores_nothing(monkeypatch):
    conn = _connection()
    monkeypatch.setattr(poller, "list_connections", lambda db: [conn])
    monkeypatch.setattr(
        poller.gmail_client,
        "list_recent_message_ids",
        lambda at, rt: ["m1", "m2"],
    )
    monkeypatch.setattr(
        poller.gmail_client,
        "fetch_message",
        lambda at, rt, mid: {
            "message_id": mid,
            "subject": "s",
            "sender": "a@b.com",
            "snippet": "hi",
            "received_at": None,
        },
    )

    # Simulate the unique-constraint dedupe: only never-seen message_ids "insert".
    seen: set[tuple[str, str]] = set()

    def fake_insert(db, *, user_id, message_id, **kwargs):
        key = (user_id, message_id)
        if key in seen:
            return False
        seen.add(key)
        return True

    monkeypatch.setattr(poller, "insert_processed_email", fake_insert)
    _use_fake_sessions(monkeypatch)

    first = poller.poll_once()
    second = poller.poll_once()

    assert first == 2  # both new
    assert second == 0  # deduped


def test_expired_token_is_refreshed(monkeypatch):
    conn = _connection()
    conn.token_expiry = datetime.now(tz=timezone.utc) - timedelta(minutes=1)  # expired
    monkeypatch.setattr(poller, "list_connections", lambda db: [conn])
    monkeypatch.setattr(poller.gmail_client, "list_recent_message_ids", lambda at, rt: [])

    refreshed = {}

    def fake_refresh(refresh_token):
        refreshed["called"] = True
        return "new-access", datetime.now(tz=timezone.utc) + timedelta(hours=1)

    monkeypatch.setattr(poller.gmail_client, "refresh_access_token", fake_refresh)
    monkeypatch.setattr(poller, "update_tokens", lambda db, **kw: None)
    _use_fake_sessions(monkeypatch)

    poller.poll_once()
    assert refreshed.get("called") is True


def test_one_failing_connection_does_not_stop_others(monkeypatch):
    good = _connection()
    bad = _connection()
    bad.user_id = "user-bad"
    monkeypatch.setattr(poller, "list_connections", lambda db: [bad, good])

    def list_ids(at, rt):
        return ["m1"]

    monkeypatch.setattr(poller.gmail_client, "list_recent_message_ids", list_ids)

    def fetch(at, rt, mid):
        return {"message_id": mid, "subject": None, "sender": None, "snippet": None, "received_at": None}

    monkeypatch.setattr(poller.gmail_client, "fetch_message", fetch)

    calls = {"n": 0}

    def fake_insert(db, *, user_id, message_id, **kwargs):
        calls["n"] += 1
        if user_id == "user-bad":
            raise RuntimeError("boom")
        return True

    monkeypatch.setattr(poller, "insert_processed_email", fake_insert)
    sessions = _use_fake_sessions(monkeypatch)

    total = poller.poll_once()
    assert total == 1  # good connection still stored despite bad one raising
    # Each connection runs in its own session; exactly the bad one is rolled back, and
    # the good connection (a separate session) is unaffected.
    assert sum(s.rollbacks for s in sessions) == 1
