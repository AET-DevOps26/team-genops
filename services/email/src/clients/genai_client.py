"""Client for genai's internal email-analysis endpoint.

Sync httpx (the poller runs in APScheduler's worker thread, not the event loop).
Authenticated with the shared INTERNAL_SERVICE_TOKEN — never a user JWT: the
pipeline acts in the background with no live user request.
"""
from __future__ import annotations

import httpx

from ..config import get_settings

_settings = get_settings()

# LLM calls are slow; generous timeout compared to the application-service client.
_TIMEOUT = 60.0


def analyze_email(
    *,
    user_id: str,
    email: dict,
    applications: list[dict],
) -> dict:
    """POST /internal/v1/email-analysis. Raises httpx errors on failure (caller retries)."""
    response = httpx.post(
        f"{_settings.genai_url}/internal/v1/email-analysis",
        json={"user_id": user_id, "email": email, "applications": applications},
        headers={"Authorization": f"Bearer {_settings.internal_service_token}"},
        timeout=_TIMEOUT,
    )
    response.raise_for_status()
    return response.json()
