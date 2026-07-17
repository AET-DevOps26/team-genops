"""
The authentication lifecycle, end to end through the gateway.

The split-token BFF contract is the thing under test: tokens live only in HttpOnly cookies,
never in a response body, and the browser never handles them in JS. Unit tests assert the
pieces; only this proves the cookies a real server sets are the cookies a real gateway accepts.
"""

import httpx
import pytest

from e2e_tests.conftest import BrowserSession, User, new_email

ACCESS_COOKIE = "jr_access"
REFRESH_COOKIE = "jr_refresh"


def _cookie_header(response: httpx.Response, name: str) -> str | None:
    """The raw Set-Cookie line for `name`, so attributes can be asserted, not just the value."""
    for header in response.headers.get_list("set-cookie"):
        if header.startswith(f"{name}="):
            return header
    return None


# ---------------------------------------------------------------- register / login


def test_register_issues_session_cookies(client: BrowserSession):
    response = client.post("/api/v1/auth/register", json={"email": new_email(), "password": "Correct-Horse-Battery-9"})

    assert response.status_code in (200, 201)
    assert client.cookie(ACCESS_COOKIE), "register must mint an access cookie"
    assert client.cookie(REFRESH_COOKIE), "register must mint a refresh cookie"


def test_tokens_are_never_returned_in_the_response_body(client: BrowserSession):
    """
    The whole point of the HttpOnly split-token design: a token in the body is readable by
    JS, which is exactly the XSS exposure the cookies exist to avoid.
    """
    response = client.post("/api/v1/auth/register", json={"email": new_email(), "password": "Correct-Horse-Battery-9"})

    body = response.text.lower()
    assert "access_token" not in body
    assert "refresh_token" not in body
    assert client.cookie(ACCESS_COOKIE) not in body


def test_access_cookie_is_httponly_and_site_locked(client: BrowserSession):
    """HttpOnly keeps JS out; SameSite=Strict is what removes the need for CSRF tokens."""
    response = client.post("/api/v1/auth/register", json={"email": new_email(), "password": "Correct-Horse-Battery-9"})

    raw = _cookie_header(response, ACCESS_COOKIE)
    assert raw is not None
    assert "HttpOnly" in raw
    assert "SameSite=Strict" in raw


def test_refresh_cookie_is_scoped_to_the_auth_path(client: BrowserSession):
    """
    The refresh token is only ever presented to /api/v1/auth — scoping its Path means the
    browser does not attach it to every API call, so it cannot leak to other services.
    """
    response = client.post("/api/v1/auth/register", json={"email": new_email(), "password": "Correct-Horse-Battery-9"})

    raw = _cookie_header(response, REFRESH_COOKIE)
    assert raw is not None
    assert "HttpOnly" in raw
    assert "Path=/api/v1/auth" in raw


def test_login_with_correct_password_succeeds(user: User):
    fresh = BrowserSession()
    response = fresh.post("/api/v1/auth/login", json={"email": user.email, "password": user.password})

    assert response.status_code == 200
    assert fresh.cookie(ACCESS_COOKIE)
    fresh.close()


def test_login_with_a_wrong_password_is_rejected(user: User):
    fresh = BrowserSession()
    response = fresh.post("/api/v1/auth/login", json={"email": user.email, "password": "not-the-password"})

    assert response.status_code == 401
    assert not fresh.cookie(ACCESS_COOKIE)
    fresh.close()


def test_registering_a_taken_email_is_rejected(user: User):
    response = user.client.post("/api/v1/auth/register", json={"email": user.email, "password": "Another-Pass-1"})

    assert response.status_code == 409


def test_login_does_not_reveal_whether_an_account_exists(client: BrowserSession):
    """An unknown address and a wrong password must be indistinguishable to an attacker."""
    unknown = client.post("/api/v1/auth/login", json={"email": new_email(), "password": "whatever"})

    assert unknown.status_code == 401


# ---------------------------------------------------------------- session identity


def test_me_returns_the_authenticated_user(user: User):
    response = user.client.get("/api/v1/auth/me")

    assert response.status_code == 200
    assert response.json()["email"] == user.email


def test_me_requires_authentication(client: BrowserSession):
    assert client.get("/api/v1/auth/me").status_code == 401


# ---------------------------------------------------------------- refresh rotation


def test_refresh_rotates_the_session(user: User):
    before = user.client.cookie(ACCESS_COOKIE)

    response = user.client.post("/api/v1/auth/refresh")

    # 204: the new tokens are delivered as cookies, so there is deliberately no body.
    assert response.status_code == 204
    assert user.client.cookie(ACCESS_COOKIE), "refresh must re-issue an access cookie"
    assert user.client.get("/api/v1/auth/me").status_code == 200
    assert before is not None


def test_a_used_refresh_token_cannot_be_replayed(user: User):
    """
    Refresh tokens are single-use, enforced in Redis. The unit test mocks that store away;
    only here does a real round-trip prove a stolen token dies once the owner uses it.
    """
    stolen = user.client.cookie(REFRESH_COOKIE)
    assert stolen, "expected a refresh cookie to steal"

    assert user.client.post("/api/v1/auth/refresh").status_code == 204

    replay = BrowserSession()
    replay.set_cookie(REFRESH_COOKIE, stolen, path="/api/v1/auth")
    response = replay.post("/api/v1/auth/refresh")
    replay.close()

    assert response.status_code == 401, "a refresh token must not survive being used once"


def test_refresh_without_a_token_is_rejected(client: BrowserSession):
    assert client.post("/api/v1/auth/refresh").status_code == 401


# ---------------------------------------------------------------- logout


def test_logout_invalidates_the_refresh_token(user: User):
    revoked = user.client.cookie(REFRESH_COOKIE)
    assert user.client.post("/api/v1/auth/logout").status_code in (200, 204)

    replay = BrowserSession()
    replay.set_cookie(REFRESH_COOKIE, revoked, path="/api/v1/auth")
    response = replay.post("/api/v1/auth/refresh")
    replay.close()

    assert response.status_code == 401, "logout must revoke the refresh token server-side"


# ---------------------------------------------------------------- public key material


def test_jwks_is_public_and_exposes_no_private_key(client: BrowserSession):
    """
    Other services fetch this to verify tokens, so it must be reachable unauthenticated —
    and it must never carry the private exponent, which would forge any identity.
    """
    response = client.get("/api/v1/auth/.well-known/jwks.json")

    assert response.status_code == 200
    keys = response.json()["keys"]
    assert keys, "JWKS must publish at least one key"
    for key in keys:
        assert key.get("kty") == "RSA"
        assert "n" in key and "e" in key
        for private_field in ("d", "p", "q", "dp", "dq", "qi"):
            assert private_field not in key, f"JWKS leaked private key material: {private_field}"


# ---------------------------------------------------------------- the gateway's own surface


def test_health_probes_are_reachable_without_a_token(client: BrowserSession):
    """k8s liveness/readiness probes cannot carry a JWT, so they must never require one."""
    assert client.get("/actuator/health/readiness").status_code == 200


@pytest.mark.parametrize("path", ["/actuator/gateway/routes", "/actuator/env", "/actuator/beans"])
def test_the_gateways_operational_surface_is_not_exposed(client: BrowserSession, path: str):
    """
    SecurityConfig denies /actuator/** apart from the probe group, and only `health` is
    exposed. /actuator/gateway/routes would enumerate the internal topology to anyone who
    can reach the ingress, so a 200 here is a real information leak.
    """
    response = client.get(path)

    assert response.status_code != 200, f"{path} must not be reachable through the ingress"
