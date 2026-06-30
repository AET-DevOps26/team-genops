"""
Client for fetching user profile from the document service.
Currently stubbed — wire up when the document service is available.
"""

import httpx

from src.config import settings

_NOT_IMPLEMENTED = (
    "User profile not available yet — document service not connected."
)


async def get_user_profile(user_id: str) -> str:
    """
    Fetch and format the user's career profile from the document service.
    Returns a plain-text representation ready to inject into the LLM context.
    """
    if not settings.profile_enabled:
        return _NOT_IMPLEMENTED

    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{settings.document_service_url}/api/v1/profile",
            headers={"X-User-Id": user_id},
            timeout=5.0,
        )
        response.raise_for_status()
        data = response.json()

    return _format_profile(data)


def _format_profile(data: dict) -> str:
    """Format the profile JSON into a readable string for LLM injection."""
    lines = ["User Profile:"]

    if data.get("name"):
        lines.append(f"Name: {data['name']}")
    if data.get("experience"):
        lines.append(f"Experience: {data['experience']}")
    if data.get("education"):
        lines.append(f"Education: {data['education']}")
    if data.get("skills"):
        skills = ", ".join(data["skills"]) if isinstance(data["skills"], list) else data["skills"]
        lines.append(f"Skills: {skills}")
    if data.get("certifications"):
        lines.append(f"Certifications: {data['certifications']}")
    if data.get("languages"):
        lines.append(f"Languages: {data['languages']}")

    return "\n".join(lines)
