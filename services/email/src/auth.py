"""JWT authentication dependency.

Each service re-verifies the access token locally against the auth service's published
JWKS (defense in depth) — see `.claude/rules/microservices.md`. The token arrives as
`Authorization: Bearer <jwt>` (the gateway translates the `jr_access` cookie into this
header; services never read cookies between each other). `user_id` is taken ONLY from the
`sub` claim — never from the body, query string, or any other header.
"""

from __future__ import annotations

import jwt
from fastapi import Depends, Request
from fastapi.security.utils import get_authorization_scheme_param
from jwt import PyJWKClient

from .config import get_settings
from .errors import unauthorized

_settings = get_settings()

# PyJWKClient fetches and caches the JWKS, refreshing when it sees an unknown `kid`.
_jwk_client = PyJWKClient(_settings.auth_jwks_url)


def _verify(token: str) -> str:
    try:
        signing_key = _jwk_client.get_signing_key_from_jwt(token)
        claims = jwt.decode(token, signing_key.key, algorithms=["RS256"])
    except Exception as exc:  # noqa: BLE001 — any failure means the token is not trusted
        raise unauthorized("Invalid or expired access token") from exc

    user_id = claims.get("sub")
    if not user_id:
        raise unauthorized("Token is missing the subject claim")
    return str(user_id)


def get_current_user_id(request: Request) -> str:
    """FastAPI dependency: returns the authenticated user's id from the JWT `sub`."""
    auth_header = request.headers.get("Authorization")
    scheme, token = get_authorization_scheme_param(auth_header)
    if not token or scheme.lower() != "bearer":
        raise unauthorized("Missing bearer token")
    return _verify(token)


CurrentUserId = Depends(get_current_user_id)
