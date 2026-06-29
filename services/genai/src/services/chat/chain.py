from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.prebuilt import create_react_agent
from psycopg import AsyncConnection

from src.llm.client import llm
from src.prompts.career_assistant.system.base import SYSTEM_PROMPT
from src.prompts.career_assistant.system.commands import resolve_command
from src.services.chat.session import is_first_user_session
from src.services.chat.utils.history import load_history, save_message
from src.services.profile_client import get_user_profile
from src.tools.profile import make_profile_tool
from src.tools.session_memory import make_session_memory_tool


async def chat(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    message: str,
) -> str:
    """
    Run one conversational turn through the LangGraph react agent.

    First session ever → profile injected directly into system prompt.
    Subsequent sessions → profile available as a tool the agent calls on demand.
    """
    task_context, cleaned_input = resolve_command(message)
    chat_history = await load_history(conn, session_id)

    if await is_first_user_session(conn, user_id, session_id):
        user_memory = await get_user_profile(user_id)
        tools = [make_session_memory_tool(conn, user_id, session_id)]
    else:
        user_memory = ""
        tools = [
            make_session_memory_tool(conn, user_id, session_id),
            make_profile_tool(user_id),
        ]

    system_msg = SystemMessage(content=SYSTEM_PROMPT.format(
        user_memory=user_memory,
        task_context=task_context,
    ))

    messages = [system_msg] + chat_history + [HumanMessage(content=cleaned_input)]

    agent = create_react_agent(llm, tools)
    response = await agent.ainvoke({"messages": messages})

    ai_content = response["messages"][-1].content

    await save_message(conn, session_id, "user", message)
    await save_message(conn, session_id, "assistant", ai_content)

    return ai_content
