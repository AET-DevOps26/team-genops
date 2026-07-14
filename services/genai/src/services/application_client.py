"""
Client for fetching a job application from the application service.

The user's own JWT is forwarded as `Authorization: Bearer` — the application
service resolves the owner from the token's `sub` claim, so a user can never
pull another user's application by guessing an id. Never send user ids in
custom headers.
"""

import logging

import httpx

from src.config import settings

logger = logging.getLogger(__name__)

# The job description is free text pasted from a posting and can be enormous. It goes into
# every turn's system prompt once a session is bound to an application, so bound it.
MAX_DESCRIPTION_CHARS = 4000

_NOT_FOUND = (
    "The referenced job application could not be found. Ask the user for the "
    "role, company and job description directly."
)

_UNAVAILABLE = (
    "The job application details could not be loaded right now. Ask the user for "
    "the role, company and job description directly."
)


async def get_application(token: str, application_id: str) -> str | None:
    """
    Fetch one application and format it for injection into the LLM context.

    Returns None only when there is nothing to say (no id). Any failure returns a
    plain-text note instead of raising: this runs on every turn of a bound session,
    so an application-service outage must degrade the answer, not fail the turn.
    """
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{settings.application_service_url}/api/v1/applications/{application_id}",
                headers={"Authorization": f"Bearer {token}"},
                timeout=5.0,
            )
            if response.status_code == 404:
                return _NOT_FOUND
            response.raise_for_status()
            data = response.json()
    except Exception:
        logger.warning("application fetch failed; continuing without it", exc_info=True)
        return _UNAVAILABLE

    return _format_application(data)


async def list_applications(token: str) -> str:
    """
    Fetch the user's applications and format them as a compact overview.

    Descriptions are omitted — this is the index the model reads to decide which application
    to look up in full, so keeping it short is the point. Never raises, for the same reason
    as get_application.
    """
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{settings.application_service_url}/api/v1/applications",
                headers={"Authorization": f"Bearer {token}"},
                timeout=5.0,
            )
            response.raise_for_status()
            data = response.json()
    except Exception:
        logger.warning("application list failed; continuing without it", exc_info=True)
        return _UNAVAILABLE

    items = data.get("items") or []
    if not items:
        return (
            "The user has no job applications tracked yet. Suggest adding one in the app so "
            "documents can be tailored to it."
        )

    lines = [f"The user is tracking {len(items)} job application(s):"]
    for item in items:
        line = f"- {item.get('job_title')} at {item.get('company')} — stage: {item.get('stage')}"
        if not item.get("job_description"):
            # Say so up front: without it a tailored document has only role + company to work
            # from, and the model should ask rather than quietly produce something generic.
            line += " (no job description saved)"
        line += f" [id: {item.get('id')}]"
        lines.append(line)

    return "\n".join(lines)


def _format_application(data: dict) -> str:
    """Format a JobApplication into a readable block for LLM injection."""
    lines = ["Target Job Application (the document must be tailored to this role):"]

    if data.get("job_title"):
        lines.append(f"Role: {data['job_title']}")
    if data.get("company"):
        lines.append(f"Company: {data['company']}")
    if data.get("stage"):
        lines.append(f"Current stage: {data['stage']}")
    if data.get("job_url"):
        lines.append(f"Posting: {data['job_url']}")
    if data.get("company_website"):
        lines.append(f"Company website: {data['company_website']}")
    if data.get("notes"):
        lines.append(f"User's notes: {data['notes']}")

    description = data.get("job_description")
    if description:
        if len(description) > MAX_DESCRIPTION_CHARS:
            description = description[:MAX_DESCRIPTION_CHARS].rstrip() + "…"
        lines.append(f"Job description:\n{description}")
    else:
        lines.append(
            "Job description: not provided — ask the user to paste it before writing "
            "the document."
        )

    return "\n".join(lines)
