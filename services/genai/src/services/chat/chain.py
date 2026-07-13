from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.prebuilt import create_react_agent
from psycopg import AsyncConnection

from src.llm.client import llm
from src.observability import trace_config
from src.prompts.career_assistant.system.base import SYSTEM_PROMPT
from src.prompts.career_assistant.system.commands import resolve_command
from src.services.chat.session import get_session_summary
from src.services.chat.utils.history import load_history, save_message
from src.services.profile_client import get_user_profile
from src.tools.documents import make_save_document_tool
from src.tools.session_memory import make_session_memory_tool


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

    tools = [
        make_session_memory_tool(conn, user_id, session_id),
        make_save_document_tool(token),
    ]

    system_msg = SystemMessage(content=SYSTEM_PROMPT.format(
        user_memory=user_memory,
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
