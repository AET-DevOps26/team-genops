from langchain_core.tools import tool
from psycopg import AsyncConnection

from src.services.chat.utils.rag import search_past_sessions as _search


def make_session_memory_tool(conn: AsyncConnection, user_id: str, session_id: str):
    @tool
    async def search_past_sessions(query: str) -> str:
        """
        Search your past conversation sessions for relevant context.
        Use this when the user references something that may have been discussed
        in a previous session, or when you need background about their experience,
        past job applications, or earlier advice given.
        Call with a specific query describing what you are looking for.
        """
        return await _search(conn, user_id, query, session_id)

    return search_past_sessions
