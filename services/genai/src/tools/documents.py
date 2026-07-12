from langchain_core.tools import tool

from src.services.document_client import save_generated_document as _save


def make_save_document_tool(token: str):
    @tool
    async def save_generated_document(
        application_id: str, document_type: str, content: str
    ) -> str:
        """
        Save a finished cover letter or resume to the user's account, attached to
        one of their job applications, so it shows up on that application's page.
        Call this ONLY after you have produced the final document text AND the
        conversation contains the application's id (a UUID, e.g. provided by the
        app when the user starts a chat from an application). Never invent an id —
        if none was provided, skip saving and just present the document.
        document_type must be exactly "cover_letter" or "resume".
        """
        if document_type not in ("cover_letter", "resume"):
            return 'Not saved: document_type must be "cover_letter" or "resume".'
        try:
            await _save(token, application_id, document_type, content)
        except Exception:
            return (
                "Saving failed — the application id may be wrong or the document "
                "service unavailable. Present the document to the user directly."
            )
        return f"Saved {document_type} to application {application_id}."

    return save_generated_document
