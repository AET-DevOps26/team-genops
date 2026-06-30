"""OAuth callback + state-token security tests (Gmail and DB mocked)."""
import time
from datetime import datetime, timezone
from unittest.mock import MagicMock

import jwt
import pytest
from fastapi.testclient import TestClient

from src import state as state_mod
from src.config import get_settings
from src.db import get_db
from src.errors import ApiError
from src.gmail_client import ExchangedCredentials
from src.main import app
from src.routers import connections as conn_router

CALLBACK = "/api/v1/email/connections/gmail/callback"


# --- state token unit tests (the trust anchor for the unauthenticated callback) ---


def test_state_roundtrip():
    token = state_mod.issue_state("user-7")
    assert state_mod.verify_state(token) == "user-7"


def test_forged_state_rejected():
    forged = jwt.encode({"sub": "attacker", "nonce": "x"}, "wrong-key", algorithm="HS256")
    with pytest.raises(ApiError) as exc:
        state_mod.verify_state(forged)
    assert exc.value.status_code == 400


def test_expired_state_rejected():
    s = get_settings()
    now = int(time.time())
    expired = jwt.encode(
        {"sub": "u", "nonce": "n", "exp": now - 10},
        s.state_signing_key,
        algorithm="HS256",
    )
    with pytest.raises(ApiError):
        state_mod.verify_state(expired)


def test_replayed_state_rejected():
    token = state_mod.issue_state("user-9")
    assert state_mod.verify_state(token) == "user-9"  # first use ok
    with pytest.raises(ApiError) as exc:  # second use blocked
        state_mod.verify_state(token)
    assert exc.value.status_code == 400


# --- callback integration (Gmail exchange + DB upsert mocked) ---


@pytest.fixture
def client():
    app.dependency_overrides[get_db] = lambda: MagicMock()
    c = TestClient(app)
    yield c
    app.dependency_overrides.clear()


def test_callback_forged_state_does_not_exchange(client, monkeypatch):
    exchange = MagicMock()
    monkeypatch.setattr(conn_router.gmail_client, "exchange_code", exchange)
    resp = client.get(CALLBACK, params={"code": "abc", "state": "forged"}, follow_redirects=False)
    assert resp.status_code == 400
    exchange.assert_not_called()


def test_callback_success_stores_connection(client, monkeypatch):
    state = state_mod.issue_state("user-123")
    monkeypatch.setattr(
        conn_router.gmail_client,
        "exchange_code",
        lambda code: ExchangedCredentials(
            email_address="jane@gmail.com",
            access_token="at",
            refresh_token="rt",
            token_expiry=datetime.now(tz=timezone.utc),
        ),
    )
    upsert = MagicMock()
    monkeypatch.setattr(conn_router, "upsert_connection", upsert)

    resp = client.get(CALLBACK, params={"code": "abc", "state": state}, follow_redirects=False)
    assert resp.status_code == 302
    assert "email_connected=1" in resp.headers["location"]
    # user_id must come from the verified state, not the request.
    assert upsert.call_args.kwargs["user_id"] == "user-123"
    assert upsert.call_args.kwargs["email_address"] == "jane@gmail.com"


def test_callback_without_refresh_token_rejected(client, monkeypatch):
    state = state_mod.issue_state("user-555")
    monkeypatch.setattr(
        conn_router.gmail_client,
        "exchange_code",
        lambda code: ExchangedCredentials(
            email_address="x@gmail.com",
            access_token="at",
            refresh_token="",
            token_expiry=datetime.now(tz=timezone.utc),
        ),
    )
    upsert = MagicMock()
    monkeypatch.setattr(conn_router, "upsert_connection", upsert)
    resp = client.get(CALLBACK, params={"code": "abc", "state": state}, follow_redirects=False)
    assert resp.status_code == 400
    upsert.assert_not_called()
