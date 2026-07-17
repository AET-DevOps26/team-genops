from langchain_core.tools import tool

from src.services.application_client import (
    get_application as _get_application,
    list_applications as _list_applications,
)


def make_application_tools(token: str):
    """
    On-demand access to the user's job applications.

    The application a chat is *about* is injected into the prompt on every turn — the model
    must never have to ask for it. These tools cover the other case: questions that range
    across applications ("which of my applications are still open?", "what did I apply for at
    Google?"), where the relevant application is not known ahead of time and pushing them all
    would waste the context.

    The user's own JWT is forwarded, so the application service enforces ownership: an id the
    model invents or borrows resolves to a 404, never to another user's data.
    """

    @tool
    async def list_job_applications() -> str:
        """
        List the user's tracked job applications — role, company, stage, and id for each.
        Use this to answer questions about their applications in general, or to find the id of
        an application they refer to by name ("my Zalando one") before looking it up in detail.
        Job descriptions are not included; call get_job_application for those.
        The ids are for your own tool calls — refer to applications by role and company when
        talking to the user, never by id.
        """
        return await _list_applications(token)

    @tool
    async def get_job_application(application_id: str) -> str:
        """
        Fetch one job application in full: role, company, stage, links, the user's notes, and
        the job description. Use it once you know which application matters — typically an id
        returned by list_job_applications. Do not invent an id.
        """
        return await _get_application(token, application_id)

    return [list_job_applications, get_job_application]
