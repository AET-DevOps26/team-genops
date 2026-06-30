from langchain_core.messages import AIMessage, HumanMessage
from psycopg import AsyncConnection


HISTORY_WINDOW = 10  # number of most recent messages passed to the LLM


async def load_history(conn: AsyncConnection, session_id: str) -> list:
    """
    Load the most recent HISTORY_WINDOW messages for a session.
    All messages are persisted in the DB — only the window is sent to the LLM
    to keep token usage bounded.
    """
    cur = await conn.execute(
        """
        SELECT role, content FROM (
            SELECT role, content, seq
            FROM genai.chat_messages
            WHERE session_id = %s
            ORDER BY seq DESC
            LIMIT %s
        ) recent
        ORDER BY seq ASC
        """,
        (session_id, HISTORY_WINDOW),
    )
    rows = await cur.fetchall()
    messages = []
    for role, content in rows:
        if role == "user":
            messages.append(HumanMessage(content=content))
        else:
            messages.append(AIMessage(content=content))
    return messages


async def save_message(conn: AsyncConnection, session_id: str, role: str, content: str) -> None:
    """Persist a single message to the DB."""
    await conn.execute(
        """
        INSERT INTO genai.chat_messages (session_id, role, content)
        VALUES (%s, %s, %s)
        """,
        (session_id, role, content),
    )


async def count_messages(conn: AsyncConnection, session_id: str) -> int:
    """Return the total number of messages in a session."""
    cur = await conn.execute(
        "SELECT COUNT(*) FROM genai.chat_messages WHERE session_id = %s",
        (session_id,),
    )
    row = await cur.fetchone()
    return row[0]


async def load_last_n_messages_as_text(
    conn: AsyncConnection, session_id: str, n: int
) -> str:
    """
    Load the last N messages as a plain text block for summarization.
    Format: 'User: ...\nAssistant: ...'
    """
    cur = await conn.execute(
        """
        SELECT role, content FROM (
            SELECT role, content, seq
            FROM genai.chat_messages
            WHERE session_id = %s
            ORDER BY seq DESC
            LIMIT %s
        ) recent
        ORDER BY seq ASC
        """,
        (session_id, n),
    )
    rows = await cur.fetchall()
    lines = []
    for role, content in rows:
        label = "User" if role == "user" else "Assistant"
        lines.append(f"{label}: {content}")
    return "\n".join(lines)
