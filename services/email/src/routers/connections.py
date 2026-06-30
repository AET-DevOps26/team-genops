"""Gmail connection endpoints: authorize, OAuth callback, status, disconnect."""
from __future__ import annotations

from fastapi import APIRouter, Depends, Response
from fastapi.responses import RedirectResponse
from sqlalchemy.orm import Session

from .. import gmail_client
from ..auth import get_current_user_id
from ..config import get_settings
from ..db import delete_connection, get_connection, get_db, upsert_connection
from ..errors import bad_request
from ..state import issue_state, verify_state

router = APIRouter(prefix="/api/v1/email/connections", tags=["Email"])
_settings = get_settings()


@router.post("/gmail/authorize")
def authorize_gmail(user_id: str = Depends(get_current_user_id)) -> dict:
    """Return a Google consent URL with a signed, single-use state bound to the user."""
    state = issue_state(user_id)
    return {"authorization_url": gmail_client.build_authorization_url(state)}


@router.get("/gmail/callback")
def gmail_callback(code: str, state: str, db: Session = Depends(get_db)) -> RedirectResponse:
    """Handle Google's redirect: verify state, exchange code, store the connection.

    Unauthenticated — the signed `state` is the trust anchor and yields the user_id.
    """
    user_id = verify_state(state)
    try:
        creds = gmail_client.exchange_code(code)
    except Exception as exc:  # noqa: BLE001
        raise bad_request("Failed to exchange authorization code") from exc

    if not creds.refresh_token:
        # Without a refresh token the poller cannot keep fetching after the access
        # token expires; force the user to re-consent.
        raise bad_request("Google did not return a refresh token; please re-connect")

    upsert_connection(
        db,
        user_id=user_id,
        provider=gmail_client.PROVIDER,
        email_address=creds.email_address,
        access_token=creds.access_token,
        refresh_token=creds.refresh_token,
        token_expiry=creds.token_expiry,
    )
    return RedirectResponse(url=f"{_settings.frontend_redirect_url}?email_connected=1", status_code=302)


@router.get("")
def get_status(
    user_id: str = Depends(get_current_user_id), db: Session = Depends(get_db)
) -> dict:
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
def disconnect(
    user_id: str = Depends(get_current_user_id), db: Session = Depends(get_db)
) -> Response:
    delete_connection(db, user_id)
    return Response(status_code=204)
