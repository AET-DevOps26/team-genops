"""Gmail OAuth2 + Gmail API access.

Wraps `google-auth-oauthlib` (consent flow + token exchange) and the Gmail API client
(list/fetch messages, token refresh). Network-touching functions are small and free of
DB/web concerns so they can be mocked in tests.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime, UTC

# Google frequently returns a scope set that differs (order/extras, e.g. an added
# `openid`) from what was requested, which makes oauthlib raise "Scope has changed"
# on token exchange. Relaxing the check is the documented workaround and is safe here
# because we request a fixed, read-only scope set.
os.environ.setdefault("OAUTHLIB_RELAX_TOKEN_SCOPE", "1")

from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build

from .config import get_settings

_settings = get_settings()

PROVIDER = "gmail"
TOKEN_URI = "https://oauth2.googleapis.com/token"
SCOPES = [
    "openid",
    "https://www.googleapis.com/auth/userinfo.email",
    "https://www.googleapis.com/auth/gmail.readonly",
]


@dataclass
class ExchangedCredentials:
    email_address: str
    access_token: str
    # Google omits the refresh token on re-consent when one was already granted; the
    # callback handles the None case by asking the user to re-connect.
    refresh_token: str | None
    token_expiry: datetime


def _client_config() -> dict:
    return {
        "web": {
            "client_id": _settings.google_client_id,
            "client_secret": _settings.google_client_secret,
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": TOKEN_URI,
            "redirect_uris": [_settings.google_redirect_uri],
        }
    }


def _build_flow() -> Flow:
    flow = Flow.from_client_config(_client_config(), scopes=SCOPES)
    flow.redirect_uri = _settings.google_redirect_uri
    return flow


def build_authorization_url(state: str) -> str:
    """Return the Google consent URL, requesting offline access for a refresh token."""
    flow = _build_flow()
    url, _ = flow.authorization_url(
        access_type="offline",
        prompt="consent",
        state=state,
    )
    return url


def exchange_code(code: str) -> ExchangedCredentials:
    """Exchange an authorization code for tokens and resolve the mailbox address."""
    flow = _build_flow()
    flow.fetch_token(code=code)
    creds = flow.credentials
    email_address = _fetch_email_address(creds)
    return ExchangedCredentials(
        email_address=email_address,
        access_token=creds.token,
        refresh_token=creds.refresh_token,
        token_expiry=_as_utc(creds.expiry),
    )


def _fetch_email_address(creds: Credentials) -> str:
    service = build("oauth2", "v2", credentials=creds, cache_discovery=False)
    return service.userinfo().get().execute()["email"]


def _credentials(access_token: str, refresh_token: str) -> Credentials:
    return Credentials(
        token=access_token,
        refresh_token=refresh_token,
        token_uri=TOKEN_URI,
        client_id=_settings.google_client_id,
        client_secret=_settings.google_client_secret,
        scopes=SCOPES,
    )


def refresh_access_token(refresh_token: str) -> tuple[str, datetime]:
    """Use a refresh token to obtain a fresh access token + expiry."""
    creds = _credentials(access_token="", refresh_token=refresh_token)
    creds.refresh(GoogleRequest())
    return creds.token, _as_utc(creds.expiry)


def list_recent_message_ids(access_token: str, refresh_token: str) -> list[str]:
    service = _gmail(access_token, refresh_token)
    resp = service.users().messages().list(userId="me", maxResults=_settings.gmail_max_results).execute()
    return [m["id"] for m in resp.get("messages", [])]


def fetch_message(access_token: str, refresh_token: str, message_id: str) -> dict:
    """Fetch a single message's metadata and normalise it for storage."""
    service = _gmail(access_token, refresh_token)
    msg = (
        service.users()
        .messages()
        .get(
            userId="me",
            id=message_id,
            format="metadata",
            metadataHeaders=["Subject", "From"],
        )
        .execute()
    )
    headers = {h["name"].lower(): h["value"] for h in msg.get("payload", {}).get("headers", [])}
    received_at = None
    internal = msg.get("internalDate")
    if internal is not None:
        received_at = datetime.fromtimestamp(int(internal) / 1000, tz=UTC)
    return {
        "message_id": msg["id"],
        "subject": headers.get("subject"),
        "sender": headers.get("from"),
        "snippet": msg.get("snippet"),
        "received_at": received_at,
    }


def _gmail(access_token: str, refresh_token: str):
    return build(
        "gmail",
        "v1",
        credentials=_credentials(access_token, refresh_token),
        cache_discovery=False,
    )


def _as_utc(value: datetime | None) -> datetime:
    """Google credentials expose a naive UTC expiry; make it timezone-aware."""
    if value is None:
        return datetime.now(tz=UTC)
    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)
    return value
