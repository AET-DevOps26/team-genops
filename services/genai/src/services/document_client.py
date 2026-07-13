"""
Client for persisting AI-generated documents (cover letters, resumes) in the
document service, tied to a job application.

Like the profile client, the user's own JWT is forwarded — the document
service stores the row under the token's `sub`.
"""

import httpx

from src.config import settings


async def save_generated_document(
    token: str,
    application_id: str,
    document_type: str,
    content: str,
) -> dict:
    """
    POST a generated document to the document service and return the stored
    representation. `document_type` is `cover_letter` or `resume` (the OpenAPI
    wire values). Raises httpx.HTTPStatusError on failure.
    """
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{settings.document_service_url}/api/v1/documents",
            headers={"Authorization": f"Bearer {token}"},
            json={
                "application_id": application_id,
                "type": document_type,
                "content": content,
            },
            timeout=5.0,
        )
        response.raise_for_status()
        return response.json()
