from pgvector import Vector
from psycopg import AsyncConnection

from src.services.chat.utils.embeddings import embed_text

TOP_K = 3  # number of past sessions to retrieve


async def search_past_sessions(
    conn: AsyncConnection,
    user_id: str,
    query: str,
    current_session_id: str,
) -> str:
    """
    Embed the query and retrieve the top K most semantically similar
    past session summaries for this user via pgvector cosine similarity.
    Excludes the current session.
    Returns a formatted string ready to inject into the LLM context.
    """
    # Bind as Vector, not a bare list: psycopg adapts a list[float] to float8[], and there is
    # no `vector <=> double precision[]` operator — the query raised UndefinedFunction, which
    # LangGraph surfaced as a failed tool call and a 500. Writes were unaffected (an assignment
    # cast to vector exists), so embeddings stored fine while every search blew up.
    query_embedding = Vector(await embed_text(query))

    cur = await conn.execute(
        """
        SELECT summary
        FROM genai.chat_sessions
        WHERE user_id = %s
          AND id != %s
          AND embedding IS NOT NULL
          AND summary IS NOT NULL
        ORDER BY embedding <=> %s
        LIMIT %s
        """,
        (user_id, current_session_id, query_embedding, TOP_K),
    )
    rows = await cur.fetchall()

    if not rows:
        return "No relevant past sessions found."

    parts = [f"Past session {i + 1}:\n{row[0]}" for i, row in enumerate(rows)]
    return "\n\n".join(parts)
