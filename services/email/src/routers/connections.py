"""Gmail connection endpoints: authorize, OAuth callback, status, disconnect."""

from __future__ import annotations

import logging
from urllib.parse import parse_qsl, urlencode, urlparse, urlunparse

from fastapi import APIRouter, Depends, Response
from fastapi.responses import RedirectResponse
from sqlalchemy.orm import Session

from .. import gmail_client
from ..auth import get_current_user_id
from ..config import get_settings
from ..db import delete_connection, get_connection, get_db, upsert_connection
from ..state import consume_state, issue_state, validate_state

router = APIRouter(prefix="/api/v1/email/connections", tags=["Email"])
logger = logging.getLogger(__name__)
_settings = get_settings()


def _frontend_redirect(**params: str) -> RedirectResponse:
    """Redirect the browser back to the frontend, merging query params safely.

    Parsing the configured URL (rather than string-concatenating ``?...``) keeps the
    redirect valid even if ``frontend_redirect_url`` already carries a query or fragment.
    """
    parts = urlparse(_settings.frontend_redirect_url)
    query = dict(parse_qsl(parts.query))
    query.update(params)
    url = urlunparse(parts._replace(query=urlencode(query)))
    return RedirectResponse(url=url, status_code=302)


@router.post("/gmail/authorize")
def authorize_gmail(user_id: str = Depends(get_current_user_id)) -> dict:
    """Return a Google consent URL with a signed, single-use state bound to the user."""
    state = issue_state(user_id)
    return {"authorization_url": gmail_client.build_authorization_url(state)}


@router.get("/gmail/callback")
def gmail_callback(code: str, state: str, db: Session = Depends(get_db)) -> RedirectResponse:
    """Handle Google's redirect: verify state, exchange code, store the connection.

    Unauthenticated — the signed `state` is the trust anchor and yields the user_id.
    This is a browser-facing endpoint, so recoverable failures redirect back to the
    frontend with an `email_error` flag rather than rendering a raw JSON error. A
    forged/expired/replayed state still hard-fails (400): it signals tampering or a
    stale link, not a user mid-flow we should bounce back to the app.
    """
    user_id, nonce, exp = validate_state(state)
    try:
        creds = gmail_client.exchange_code(code)
    except Exception:  # noqa: BLE001
        logger.exception("Code exchange failed for user %s", user_id)
        return _frontend_redirect(email_error="exchange_failed")

    if not creds.refresh_token:
        # Without a refresh token the poller cannot keep fetching after the access
        # token expires; send the user back to re-consent.
        return _frontend_redirect(email_error="missing_refresh_token")

    # Only burn the single-use nonce now that the exchange has actually succeeded, so a
    # transient Google failure leaves the link reusable instead of forcing a restart.
    consume_state(nonce, exp)
    upsert_connection(
        db,
        user_id=user_id,
        provider=gmail_client.PROVIDER,
        email_address=creds.email_address,
        access_token=creds.access_token,
        refresh_token=creds.refresh_token,
        token_expiry=creds.token_expiry,
    )
    return _frontend_redirect(email_connected="1")


@router.get("")
def get_status(user_id: str = Depends(get_current_user_id), db: Session = Depends(get_db)) -> dict:
    conn = get_connection(db, user_id)
    if conn is None:
        return {"connected": False}
    return {
        "connected": True,
        # Only Gmail is implemented today; the OpenAPI enum is [gmail]. Return the
        # constant rather than the raw DB value so the response can't drift from the
        # published contract if other providers are added to the schema later.
        "provider": gmail_client.PROVIDER,
        "email_address": conn.email_address,
        "connected_at": conn.created_at,
    }


@router.delete("", status_code=204, response_class=Response)
def disconnect(user_id: str = Depends(get_current_user_id), db: Session = Depends(get_db)) -> Response:
    delete_connection(db, user_id)
    return Response(status_code=204)
