"""
JWT verification for incoming requests.
The browser sends the access token as the HttpOnly `jr_access` cookie; internal
callers may instead send an `Authorization: Bearer` header. We accept either and
verify the token using the auth service's public key (JWKS).
"""

import secrets
import time

import httpx
from fastapi import HTTPException, Request, status
from jose import JWTError, jwt

from src.config import settings

# Name of the HttpOnly access-token cookie set by the auth service at the browser edge.
_ACCESS_COOKIE = "jr_access"
_jwks_cache: dict | None = None
_jwks_cache_time: float = 0
_JWKS_CACHE_TTL = 300  # 5 minutes


async def _get_jwks(force_refresh: bool = False) -> dict:
    """
    Fetch JWKS with TTL-based caching.
    force_refresh=True bypasses cache and re-fetches from auth service.
    """
    global _jwks_cache, _jwks_cache_time
    now = time.time()
    if not force_refresh and _jwks_cache is not None and (now - _jwks_cache_time) < _JWKS_CACHE_TTL:
        return _jwks_cache

    async with httpx.AsyncClient() as client:
        response = await client.get(settings.auth_jwks_url, timeout=5.0)
        response.raise_for_status()
        _jwks_cache = response.json()
        _jwks_cache_time = now
    return _jwks_cache


def _extract_token(request: Request) -> str:
    """
    Resolve the access token from the `jr_access` cookie (the form the browser sends),
    falling back to the standard `Authorization: Bearer` header (the form internal
    callers use). Raises 401 if neither is present.
    """
    cookie_token = request.cookies.get(_ACCESS_COOKIE)
    if cookie_token:
        return cookie_token

    auth_header = request.headers.get("Authorization", "")
    scheme, _, param = auth_header.partition(" ")
    if scheme.lower() == "bearer" and param:
        return param

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Not authenticated",
    )


async def get_current_user_id(request: Request) -> str:
    """
    FastAPI dependency — verifies the JWT and returns the user_id (sub claim).
    Raises 401 if the token is missing, invalid, or expired.
    Retries once with a fresh JWKS fetch if verification fails (handles key rotation).
    """
    token = _extract_token(request)
    try:
        jwks = await _get_jwks()
        payload = jwt.decode(
            token,
            jwks,
            algorithms=["RS256"],
            issuer=settings.auth_jwt_issuer,
            audience=settings.auth_jwt_audience,
        )
        user_id: str = payload.get("sub")
        if not user_id:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
        return user_id
    except JWTError as first_error:
        # Token validation failed — might be due to stale JWKS after key rotation.
        # Retry once with force-refreshed JWKS before giving up.
        try:
            jwks = await _get_jwks(force_refresh=True)
            payload = jwt.decode(
                token,
                jwks,
                algorithms=["RS256"],
                issuer=settings.auth_jwt_issuer,
                audience=settings.auth_jwt_audience,
            )
            user_id = payload.get("sub")
            if not user_id:
                raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
            return user_id
        except JWTError:
            # Still failed after refresh — token is genuinely invalid or expired
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired token") from first_error


async def require_internal_token(request: Request) -> None:
    """
    FastAPI dependency for /internal/** routes — verifies the static service token
    (`INTERNAL_SERVICE_TOKEN`) sent as `Authorization: Bearer` by trusted backend
    callers (the email service). Unlike user routes there is no JWT: the caller acts
    in the background with no live user request. Rejects everything when no token is
    configured, so the internal API is fail-closed.
    """
    auth_header = request.headers.get("Authorization", "")
    scheme, _, presented = auth_header.partition(" ")
    if not settings.internal_service_token or scheme.lower() != "bearer" or not secrets.compare_digest(presented, settings.internal_service_token):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid internal service token",
        )


async def get_access_token(request: Request) -> str:
    """
    FastAPI dependency — verifies the JWT and returns the RAW token so it can be
    forwarded to downstream services (e.g. the document service) as the user.
    Downstream services re-verify it themselves (defense in depth).
    """
    await get_current_user_id(request)
    return _extract_token(request)
