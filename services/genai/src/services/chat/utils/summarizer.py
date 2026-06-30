from psycopg import AsyncConnection

from src.llm.client import llm
from src.prompts.summarization import SUMMARIZATION_PROMPT
from src.services.chat.utils.embeddings import embed_text
from src.services.chat.utils.history import HISTORY_WINDOW, load_last_n_messages_as_text

NO_SUMMARY = "NO_SUMMARY"


async def maybe_summarize(conn: AsyncConnection, session_id: str, message_count: int) -> None:
    """
    Triggered after every message save.
    Summarizes the last HISTORY_WINDOW messages at every 10th message boundary.
    If the LLM deems the segment worthless, nothing is written.
    """
    if message_count % HISTORY_WINDOW != 0:
        return

    # Capture the exact transcript at trigger time to prevent drift if new messages arrive
    # before the summarization completes
    conversation = await load_last_n_messages_as_text(conn, session_id, HISTORY_WINDOW)

    chain = SUMMARIZATION_PROMPT | llm
    response = await chain.ainvoke({"conversation": conversation})
    new_summary = response.content.strip()

    if new_summary == NO_SUMMARY:
        return

    await _append_and_embed(conn, session_id, new_summary)


async def _append_and_embed(conn: AsyncConnection, session_id: str, new_summary: str) -> None:
    """
    Append the new summary segment to the existing session summary,
    then re-embed the full updated summary and store both.
    """
    cur = await conn.execute(
        "SELECT summary FROM genai.chat_sessions WHERE id = %s",
        (session_id,),
    )
    row = await cur.fetchone()
    existing = row[0] if row and row[0] else ""

    full_summary = f"{existing}\n\n{new_summary}".strip()
    embedding = await embed_text(full_summary)

    await conn.execute(
        """
        UPDATE genai.chat_sessions
        SET summary = %s, embedding = %s, updated_at = NOW()
        WHERE id = %s
        """,
        (full_summary, embedding, session_id),
    )
