import re

from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.prebuilt import create_react_agent
from psycopg import AsyncConnection

from src.llm.client import llm
from src.observability import trace_config
from src.prompts.career_assistant.system.base import SYSTEM_PROMPT
from src.prompts.career_assistant.system.commands import resolve_command
from src.services.application_client import get_application
from src.services.chat.session import (
    bind_session_application,
    get_session_application_id,
    get_session_summary,
)
from src.services.chat.utils.history import load_history, save_message
from src.services.profile_client import get_user_profile
from src.tools.applications import make_application_tools
from src.tools.session_memory import make_session_memory_tool

# The app hands off an application by embedding its id in the message it prefills
# ("...(application id: <uuid>)"). Any bare UUID in the text is treated as that reference.
_UUID_RE = re.compile(
    r"\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b",
    re.IGNORECASE,
)


async def chat(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    message: str,
    token: str,
) -> str:
    """
    Run one conversational turn through the LangGraph react agent.

    The profile is injected into the system prompt on every turn. It used to be
    injected only in a user's first-ever session and offered as a tool thereafter,
    which meant the assistant knew who it was talking to only if the model chose to
    call that tool — so later sessions silently produced generic advice. Injecting
    also costs less than a tool call the model actually makes (that pays for the
    same tokens plus an extra inference round-trip), and re-reading each turn picks
    up profile edits made mid-session.

    `token` is the caller's verified access JWT, forwarded to the document
    service (profile reads, generated-document writes) so ownership is always
    derived from the JWT `sub` downstream.
    """
    # Verify session ownership — return 404 for both missing and not-owned
    # to avoid leaking session existence to other users
    cur = await conn.execute(
        "SELECT 1 FROM genai.chat_sessions WHERE id = %s AND user_id = %s",
        (session_id, user_id),
    )
    if not await cur.fetchone():
        from fastapi import HTTPException, status
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Session not found")

    task_context, cleaned_input = resolve_command(message)
    chat_history = await load_history(conn, session_id)

    user_memory = await get_user_profile(token)

    # Job context. The id may arrive in this message (the app prefills it when a chat is
    # started from an application); once seen it is bound to the session, so every later
    # turn — "make it shorter", "more technical" — still knows which job we are writing for.
    # Fetched rather than trusted from the message: the application service checks ownership
    # against the JWT, so a user cannot pull someone else's application by pasting its id.
    application_id = await get_session_application_id(conn, session_id)
    if application_id is None:
        match = _UUID_RE.search(message)
        if match:
            application_id = match.group(0)
            await bind_session_application(conn, session_id, application_id)

    job_context = await get_application(token, application_id) if application_id else ""

    # Messages older than HISTORY_WINDOW are not replayed verbatim, but the summarizer has
    # already compressed them into the session's rolling summary. Injecting it means falling
    # out of the window costs detail, not the fact that something was said — without it, a
    # job description pasted 20 messages ago would simply cease to exist for the model.
    summary = await get_session_summary(conn, session_id)
    session_memory = (
        f"Earlier in this conversation (summary of messages no longer shown verbatim):\n{summary}"
        if summary
        else ""
    )

    # Pushed above: who the user is, and the job this chat is about. Pulled here: everything
    # optional — other applications, past conversations. A tool only fires when the model
    # thinks to call it, which is fine for context it can live without.
    #
    # No save tool: the model writes the document, the user decides whether to keep it.
    # Saving used to happen inside the agent, so a letter was persisted before anyone had
    # read it — every draft and revision left a row. The UI now offers an explicit Save on
    # the message, which posts to the document service directly.
    tools = [
        make_session_memory_tool(conn, user_id, session_id),
        *make_application_tools(token),
    ]

    system_msg = SystemMessage(content=SYSTEM_PROMPT.format(
        user_memory=user_memory,
        job_context=job_context,
        session_memory=session_memory,
        task_context=task_context,
    ))

    messages = [system_msg] + chat_history + [HumanMessage(content=cleaned_input)]

    agent = create_react_agent(llm, tools)
    response = await agent.ainvoke(
        {"messages": messages},
        config=trace_config(user_id=user_id, session_id=session_id, tags=["chat"]),
    )

    ai_content = response["messages"][-1].content

    await save_message(conn, session_id, "user", message)
    await save_message(conn, session_id, "assistant", ai_content)

    return ai_content
