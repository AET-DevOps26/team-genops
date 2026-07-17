"""
Client for fetching the user's profile from the document service.

The user's own JWT is forwarded as `Authorization: Bearer` — the document
service resolves the owner from the token's `sub` claim, exactly like a
browser request. Never send user ids in custom headers.
"""

import logging

import httpx

from src.config import settings

logger = logging.getLogger(__name__)

# Bounds on the injected profile. It now goes into every turn's system prompt, so a
# pathological profile (dozens of roles, essay-length descriptions) must not be able to
# crowd out the conversation or blow up cost.
MAX_ENTRIES = 10
MAX_DESCRIPTION_CHARS = 600

_NOT_ENABLED = "User profile not available yet — document service not connected."

_NO_PROFILE = (
    "The user has not created a career profile yet. Ask them for the details "
    "you need (experience, education, skills), and suggest completing their "
    "profile in the app so future documents can be tailored automatically."
)

_UNAVAILABLE = "The user's career profile could not be loaded right now. Continue helping them, and ask for any background details you need."


async def get_user_profile(token: str) -> str:
    """
    Fetch and format the user's career profile from the document service.
    Returns a plain-text representation ready to inject into the LLM context.
    `token` is the caller's verified access JWT, forwarded as-is.

    Never raises: this runs on every chat turn, so a document-service outage must
    degrade the answer, not fail the turn.
    """
    if not settings.profile_enabled:
        return _NOT_ENABLED

    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{settings.document_service_url}/api/v1/profile",
                headers={"Authorization": f"Bearer {token}"},
                timeout=5.0,
            )
            if response.status_code == 404:
                # 404 is the contract's "no profile yet" signal, not an error.
                return _NO_PROFILE
            response.raise_for_status()
            data = response.json()
    except Exception:
        logger.warning("profile fetch failed; continuing without it", exc_info=True)
        return _UNAVAILABLE

    return _format_profile(data)


async def fetch_profile(token: str) -> dict | None:
    """
    Fetch the raw profile aggregate, or None if there is no profile / it is unreachable.

    For the mock-interview gate, which must decide whether a usable profile exists — unlike
    get_user_profile, which formats prose for the chat prompt and never fails a turn.
    """
    if not settings.profile_enabled:
        return None
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{settings.document_service_url}/api/v1/profile",
                headers={"Authorization": f"Bearer {token}"},
                timeout=5.0,
            )
            if response.status_code == 404:
                return None
            response.raise_for_status()
            return response.json()
    except Exception:
        logger.warning("profile fetch (raw) failed", exc_info=True)
        return None


def profile_is_complete(data: dict) -> bool:
    """
    A profile is 'complete enough' to interview against when it names the candidate and has at
    least one substantive section (experience, education, or skills). The interviewer needs
    something real to personalise and probe — an empty shell would make questions generic.
    """
    profile = data.get("profile") or {}
    has_name = bool((profile.get("first_name") or "").strip() or (profile.get("last_name") or "").strip())
    has_substance = bool(data.get("work_experiences") or data.get("educations") or data.get("skills"))
    return has_name and has_substance


def _clamp(text: str | None) -> str | None:
    """Truncate a free-text field so one verbose entry cannot dominate the context."""
    if not text or len(text) <= MAX_DESCRIPTION_CHARS:
        return text
    return text[:MAX_DESCRIPTION_CHARS].rstrip() + "…"


def _format_profile(data: dict) -> str:
    """Format a ProfileAggregateResponse into a readable string for LLM injection."""
    profile = data.get("profile") or {}
    lines = ["User Profile:"]

    name = " ".join(filter(None, [profile.get("first_name"), profile.get("last_name")]))
    if name:
        lines.append(f"Name: {name}")
    if profile.get("location"):
        lines.append(f"Location: {profile['location']}")
    if profile.get("bio"):
        lines.append(f"Summary: {_clamp(profile['bio'])}")
    if profile.get("website"):
        lines.append(f"Website: {profile['website']}")

    experiences = (data.get("work_experiences") or [])[:MAX_ENTRIES]
    if experiences:
        lines.append("Work Experience:")
        for exp in experiences:
            period = _period(exp.get("start_date"), exp.get("end_date"), exp.get("is_current"))
            entry = f"- {exp.get('role')} at {exp.get('company')} ({period})"
            if exp.get("location"):
                entry += f", {exp['location']}"
            lines.append(entry)
            if exp.get("description"):
                lines.append(f"  {_clamp(exp['description'])}")

    educations = (data.get("educations") or [])[:MAX_ENTRIES]
    if educations:
        lines.append("Education:")
        for edu in educations:
            period = _period(edu.get("start_date"), edu.get("end_date"), False)
            entry = f"- {edu.get('degree')}"
            if edu.get("field"):
                entry += f" in {edu['field']}"
            entry += f", {edu.get('institution')} ({period})"
            lines.append(entry)
            if edu.get("description"):
                lines.append(f"  {_clamp(edu['description'])}")

    skills = data.get("skills") or []
    if skills:
        formatted = ", ".join(f"{s.get('name')} ({s.get('level')})" for s in skills)
        lines.append(f"Skills: {formatted}")

    languages = data.get("languages") or []
    if languages:
        formatted = ", ".join(f"{lang.get('name')} ({lang.get('proficiency')})" for lang in languages)
        lines.append(f"Languages: {formatted}")

    return "\n".join(lines)


def _period(start: str | None, end: str | None, is_current: bool | None) -> str:
    """Human-readable date range, e.g. '2023-01 – present'."""
    start_text = start or "?"
    end_text = "present" if is_current else (end or "present")
    return f"{start_text} – {end_text}"
