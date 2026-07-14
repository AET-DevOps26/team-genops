"""Client for the application service's internal API (candidates + email updates).

Sync httpx, authenticated with the shared INTERNAL_SERVICE_TOKEN (see genai_client
for why no user JWT is involved). The contract is documented in
services/application/README.md.
"""
from __future__ import annotations

import httpx

from ..config import get_settings

_settings = get_settings()

_TIMEOUT = 5.0


def _headers() -> dict:
    return {"Authorization": f"Bearer {_settings.internal_service_token}"}


def list_applications(user_id: str) -> list[dict]:
    """The user's applications, slimmed to matching candidates."""
    response = httpx.get(
        f"{_settings.application_service_url}/internal/v1/users/{user_id}/applications",
        headers=_headers(),
        timeout=_TIMEOUT,
    )
    response.raise_for_status()
    return response.json()


def apply_email_update(
    *,
    application_id: str,
    user_id: str,
    source_message_id: str,
    suggested_stage: str | None,
    event: dict,
    recommendations: list[dict],
) -> bool:
    """Apply one email's derived update atomically. Returns the service's `applied` flag
    (False = idempotent replay)."""
    response = httpx.post(
        f"{_settings.application_service_url}/internal/v1/applications/{application_id}/email-update",
        json={
            "userId": user_id,
            "sourceMessageId": source_message_id,
            "suggestedStage": suggested_stage,
            "event": event,
            "recommendations": recommendations,
        },
        headers=_headers(),
        timeout=_TIMEOUT,
    )
    response.raise_for_status()
    return bool(response.json().get("applied"))
