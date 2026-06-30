"""Signed, single-use OAuth `state` tokens.

The Gmail callback is unauthenticated (the browser, not a service, calls it), so the
`state` is the only thing binding the flow to a user. We mint it as a short-lived HS256
JWT over the user id plus a random nonce, signed with `STATE_SIGNING_KEY`. The callback
verifies the signature + expiry and consumes the nonce exactly once, which blocks an
attacker from forging a callback to bind their Gmail to a victim's account, and blocks
replay of a captured callback.

Consumed nonces are tracked in-process (single-instance, like the poller). A multi-replica
deployment would move this to the shared Redis instance.
"""
from __future__ import annotations

import time
import uuid

import jwt

from .config import get_settings
from .errors import bad_request

_settings = get_settings()
_ALGO = "HS256"

# nonce -> expiry epoch seconds; pruned lazily on each consume.
_consumed: dict[str, float] = {}


def issue_state(user_id: str) -> str:
    now = int(time.time())
    payload = {
        "sub": user_id,
        "nonce": uuid.uuid4().hex,
        "iat": now,
        "exp": now + _settings.state_ttl_seconds,
    }
    return jwt.encode(payload, _settings.state_signing_key, algorithm=_ALGO)


def _prune(now: float) -> None:
    for nonce, exp in list(_consumed.items()):
        if exp < now:
            del _consumed[nonce]


def verify_state(state: str) -> str:
    """Validate a state token and return the bound user_id. Single-use.

    Raises ApiError(400) on a forged, expired, malformed, or replayed token.
    """
    try:
        payload = jwt.decode(state, _settings.state_signing_key, algorithms=[_ALGO])
    except Exception as exc:  # noqa: BLE001
        raise bad_request("Invalid or expired OAuth state") from exc

    user_id = payload.get("sub")
    nonce = payload.get("nonce")
    if not user_id or not nonce:
        raise bad_request("Malformed OAuth state")

    now = time.time()
    _prune(now)
    if nonce in _consumed:
        raise bad_request("OAuth state has already been used")
    _consumed[nonce] = payload.get("exp", now + _settings.state_ttl_seconds)

    return str(user_id)
