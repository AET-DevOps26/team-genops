"""JWT auth dependency tests — verifies tokens against a (mocked) JWKS signing key."""

from types import SimpleNamespace

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient

from src import auth
from src.main import app

AUTHORIZE = "/api/v1/email/connections/gmail/authorize"


@pytest.fixture
def rsa_key():
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


@pytest.fixture
def client(rsa_key, monkeypatch):
    # Make the service's JWKS client return our public key for any token.
    monkeypatch.setattr(
        auth._jwk_client,
        "get_signing_key_from_jwt",
        lambda token: SimpleNamespace(key=rsa_key.public_key()),
    )
    return TestClient(app)


def _token(rsa_key, **claims):
    return jwt.encode(claims, rsa_key, algorithm="RS256")


def test_missing_header_is_401(client):
    resp = client.post(AUTHORIZE)
    assert resp.status_code == 401
    body = resp.json()
    assert body["code"] == "UNAUTHORIZED"
    assert "message" in body


def test_wrong_scheme_is_401(client):
    resp = client.post(AUTHORIZE, headers={"Authorization": "Basic abc"})
    assert resp.status_code == 401


def test_invalid_signature_is_401(client, rsa_key):
    other = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    token = _token(other, sub="user-1")
    resp = client.post(AUTHORIZE, headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 401


def test_missing_sub_is_401(client, rsa_key):
    token = _token(rsa_key, foo="bar")
    resp = client.post(AUTHORIZE, headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 401


def test_valid_token_authorizes_and_binds_sub(client, rsa_key):
    token = _token(rsa_key, sub="user-42")
    resp = client.post(AUTHORIZE, headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    url = resp.json()["authorization_url"]
    assert url.startswith("https://accounts.google.com/o/oauth2/auth")
    # The state in the URL must be bound to this user (sub), never a guessable value.
    from urllib.parse import parse_qs, urlparse

    from src.state import verify_state

    state = parse_qs(urlparse(url).query)["state"][0]
    assert verify_state(state) == "user-42"
