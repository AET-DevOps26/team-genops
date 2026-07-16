from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.prebuilt import create_react_agent
from psycopg import AsyncConnection

from src.llm.client import llm
from src.observability import trace_config
from src.prompts.career_assistant.system.base import SYSTEM_PROMPT
from src.prompts.career_assistant.system.commands import resolve_command
from src.services.chat.session import is_first_user_session
from src.services.chat.utils.history import load_history, save_message
from src.services.profile_client import get_user_profile
from src.tools.documents import make_save_document_tool
from src.tools.profile import make_profile_tool
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

    First session ever → profile injected directly into system prompt.
    Subsequent sessions → profile available as a tool the agent calls on demand.

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

    if await is_first_user_session(conn, user_id, session_id):
        user_memory = await get_user_profile(token)
        tools = [
            make_session_memory_tool(conn, user_id, session_id),
            make_save_document_tool(token),
        ]
    else:
        user_memory = ""
        tools = [
            make_session_memory_tool(conn, user_id, session_id),
            make_profile_tool(token),
            make_save_document_tool(token),
        ]

    system_msg = SystemMessage(
        content=SYSTEM_PROMPT.format(
            user_memory=user_memory,
            task_context=task_context,
        )
    )

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
