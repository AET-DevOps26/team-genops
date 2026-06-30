"""
JWT verification for incoming requests.
The gateway translates the jr_access cookie into an Authorization: Bearer header.
We verify the token using the auth service's public key (JWKS).
"""

import time

import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from src.config import settings

_bearer = HTTPBearer()
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


async def get_current_user_id(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> str:
    """
    FastAPI dependency — verifies the Bearer JWT and returns the user_id (sub claim).
    Raises 401 if the token is missing, invalid, or expired.
    Retries once with a fresh JWKS fetch if verification fails (handles key rotation).
    """
    token = credentials.credentials
    try:
        jwks = await _get_jwks()
        payload = jwt.decode(token, jwks, algorithms=["RS256"])
        user_id: str = payload.get("sub")
        if not user_id:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
        return user_id
    except JWTError as first_error:
        # Token validation failed — might be due to stale JWKS after key rotation.
        # Retry once with force-refreshed JWKS before giving up.
        try:
            jwks = await _get_jwks(force_refresh=True)
            payload = jwt.decode(token, jwks, algorithms=["RS256"])
            user_id: str = payload.get("sub")
            if not user_id:
                raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
            return user_id
        except JWTError:
            # Still failed after refresh — token is genuinely invalid or expired
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired token"
            ) from first_error
