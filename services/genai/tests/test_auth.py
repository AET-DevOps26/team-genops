"""
JWT verification.

Tokens are signed with real RSA keys and verified against a real JWKS document, so
these exercise the actual jose verification path rather than asserting against a
stubbed decode — a mocked verifier would still pass if the algorithm allow-list or
the signature check were removed.
"""

import time
from types import SimpleNamespace
from typing import Any

import httpx
import pytest
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi import HTTPException
from jose import jwk
from jose import jwt as jose_jwt


def _keypair() -> tuple[str, str]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    private_pem = key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    ).decode()
    public_pem = (
        key.public_key()
        .public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo,
        )
        .decode()
    )
    return private_pem, public_pem


def _jwks_for(public_pem: str, kid: str = "k1") -> dict:
    entry = jwk.construct(public_pem, algorithm="RS256").to_dict()
    entry = {k: (v.decode() if isinstance(v, bytes) else v) for k, v in entry.items()}
    entry.update({"kid": kid, "use": "sig", "alg": "RS256"})
    return {"keys": [entry]}


def _token(private_pem: str, claims: dict, kid: str = "k1") -> str:
    return jose_jwt.encode(claims, private_pem, algorithm="RS256", headers={"kid": kid})


# One keypair for the whole module — RSA generation is slow enough to matter per-test.
PRIVATE_PEM, PUBLIC_PEM = _keypair()
JWKS = _jwks_for(PUBLIC_PEM)


def _request(cookies: dict | None = None, headers: dict | None = None) -> Any:
    """
    A stand-in for a Starlette Request.

    The code under test reads only `.cookies` and `.headers`; constructing a real Request
    means building an ASGI scope for no added coverage. Typed Any so the double can pass
    where a Request is expected — deliberate, and scoped to tests.
    """
    return SimpleNamespace(cookies=cookies or {}, headers=headers or {})


@pytest.fixture(autouse=True)
def _reset_jwks_cache():
    """The JWKS cache is module state; leaking it across tests hides refetch bugs."""
    import src.auth as auth

    auth._jwks_cache = None
    auth._jwks_cache_time = 0
    yield
    auth._jwks_cache = None
    auth._jwks_cache_time = 0


def _valid_claims(sub: str = "user-123") -> dict:
    now = int(time.time())
    return {"sub": sub, "iat": now, "exp": now + 300}


# ---------------------------------------------------------------- token extraction


def test_extract_token_prefers_cookie_over_header():
    from src.auth import _extract_token

    request = _request(cookies={"jr_access": "cookie-token"}, headers={"Authorization": "Bearer header-token"})

    assert _extract_token(request) == "cookie-token"


def test_extract_token_falls_back_to_bearer_header():
    from src.auth import _extract_token

    assert _extract_token(_request(headers={"Authorization": "Bearer header-token"})) == "header-token"


def test_extract_token_is_case_insensitive_about_the_scheme():
    from src.auth import _extract_token

    assert _extract_token(_request(headers={"Authorization": "bearer header-token"})) == "header-token"


@pytest.mark.parametrize(
    "headers",
    [
        {},
        {"Authorization": ""},
        {"Authorization": "Bearer"},  # scheme with no token
        {"Authorization": "Bearer "},  # empty param
        {"Authorization": "Basic abc123"},  # wrong scheme
    ],
)
def test_extract_token_rejects_anything_that_is_not_a_bearer_token(headers: dict):
    from src.auth import _extract_token

    with pytest.raises(HTTPException) as exc:
        _extract_token(_request(headers=headers))

    assert exc.value.status_code == 401


# ---------------------------------------------------------------- verification


@pytest.mark.asyncio
async def test_valid_token_returns_sub(mock_http):
    from src.auth import get_current_user_id

    mock_http(lambda request: httpx.Response(200, json=JWKS))
    token = _token(PRIVATE_PEM, _valid_claims("user-123"))

    assert await get_current_user_id(_request(cookies={"jr_access": token})) == "user-123"


@pytest.mark.asyncio
async def test_token_without_sub_is_rejected(mock_http):
    from src.auth import get_current_user_id

    mock_http(lambda request: httpx.Response(200, json=JWKS))
    now = int(time.time())
    token = _token(PRIVATE_PEM, {"iat": now, "exp": now + 300})

    with pytest.raises(HTTPException) as exc:
        await get_current_user_id(_request(cookies={"jr_access": token}))

    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_expired_token_is_rejected(mock_http):
    from src.auth import get_current_user_id

    mock_http(lambda request: httpx.Response(200, json=JWKS))
    now = int(time.time())
    token = _token(PRIVATE_PEM, {"sub": "user-123", "iat": now - 600, "exp": now - 300})

    with pytest.raises(HTTPException) as exc:
        await get_current_user_id(_request(cookies={"jr_access": token}))

    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_token_signed_by_an_unknown_key_is_rejected(mock_http):
    """A token from a key the JWKS does not publish must not authenticate."""
    from src.auth import get_current_user_id

    other_private, _ = _keypair()
    mock_http(lambda request: httpx.Response(200, json=JWKS))
    token = _token(other_private, _valid_claims())

    with pytest.raises(HTTPException) as exc:
        await get_current_user_id(_request(cookies={"jr_access": token}))

    assert exc.value.status_code == 401


@pytest.mark.asyncio
async def test_garbage_token_is_rejected(mock_http):
    from src.auth import get_current_user_id

    mock_http(lambda request: httpx.Response(200, json=JWKS))

    with pytest.raises(HTTPException) as exc:
        await get_current_user_id(_request(cookies={"jr_access": "not-a-jwt"}))

    assert exc.value.status_code == 401


# ---------------------------------------------------------------- JWKS caching / rotation


@pytest.mark.asyncio
async def test_jwks_is_cached_across_calls(mock_http):
    """The JWKS is fetched once per TTL, not once per request."""
    from src.auth import get_current_user_id

    seen = mock_http(lambda request: httpx.Response(200, json=JWKS))
    token = _token(PRIVATE_PEM, _valid_claims())
    request = _request(cookies={"jr_access": token})

    await get_current_user_id(request)
    await get_current_user_id(request)

    assert len(seen) == 1


@pytest.mark.asyncio
async def test_stale_cache_is_refreshed_once_after_key_rotation(mock_http):
    """
    The signing key rotating must not require a restart: the first decode fails against
    the cached JWKS, and the retry re-fetches and succeeds.
    """
    import src.auth as auth

    rotated_private, rotated_public = _keypair()
    rotated_jwks = _jwks_for(rotated_public, kid="k2")

    # Prime the cache with the pre-rotation JWKS.
    auth._jwks_cache = JWKS
    auth._jwks_cache_time = time.time()

    seen = mock_http(lambda request: httpx.Response(200, json=rotated_jwks))
    token = _token(rotated_private, _valid_claims("user-rotated"), kid="k2")

    result = await auth.get_current_user_id(_request(cookies={"jr_access": token}))

    assert result == "user-rotated"
    assert len(seen) == 1, "expected exactly one forced refresh"


@pytest.mark.asyncio
async def test_expired_cache_triggers_a_refetch(mock_http):
    import src.auth as auth

    auth._jwks_cache = JWKS
    auth._jwks_cache_time = time.time() - (auth._JWKS_CACHE_TTL + 1)

    seen = mock_http(lambda request: httpx.Response(200, json=JWKS))
    token = _token(PRIVATE_PEM, _valid_claims())

    await auth.get_current_user_id(_request(cookies={"jr_access": token}))

    assert len(seen) == 1


# ---------------------------------------------------------------- token forwarding


@pytest.mark.asyncio
async def test_get_access_token_returns_the_raw_token_after_verifying(mock_http):
    """Downstream services re-verify, so the exact token must be forwarded unchanged."""
    from src.auth import get_access_token

    mock_http(lambda request: httpx.Response(200, json=JWKS))
    token = _token(PRIVATE_PEM, _valid_claims())

    assert await get_access_token(_request(cookies={"jr_access": token})) == token


@pytest.mark.asyncio
async def test_get_access_token_rejects_an_invalid_token(mock_http):
    """It must not hand a token downstream without verifying it first."""
    from src.auth import get_access_token

    mock_http(lambda request: httpx.Response(200, json=JWKS))

    with pytest.raises(HTTPException) as exc:
        await get_access_token(_request(cookies={"jr_access": "not-a-jwt"}))

    assert exc.value.status_code == 401
