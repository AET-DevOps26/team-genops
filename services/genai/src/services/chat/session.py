from psycopg import AsyncConnection


async def create_session(
    conn: AsyncConnection,
    user_id: str,
    session_type: str = "insight_chat",
) -> dict:
    """Create a new chat session and return its metadata."""
    cur = await conn.execute(
        """
        INSERT INTO genai.chat_sessions (user_id, session_type)
        VALUES (%s, %s)
        RETURNING id, user_id, session_type, created_at
        """,
        (user_id, session_type),
    )
    row = await cur.fetchone()
    return {
        "id": str(row[0]),
        "user_id": str(row[1]),
        "session_type": row[2],
        "created_at": row[3].isoformat(),
    }


async def is_first_user_session(conn: AsyncConnection, user_id: str, session_id: str) -> bool:
    """Return True if this is the only session this user has ever created."""
    cur = await conn.execute(
        """
        SELECT COUNT(*) FROM genai.chat_sessions
        WHERE user_id = %s AND id != %s
        """,
        (user_id, session_id),
    )
    row = await cur.fetchone()
    return row[0] == 0


async def delete_session(conn: AsyncConnection, session_id: str, user_id: str) -> bool:
    """Delete a session (and all its messages via CASCADE). Returns True if found and deleted."""
    cur = await conn.execute(
        """
        DELETE FROM genai.chat_sessions
        WHERE id = %s AND user_id = %s
        """,
        (session_id, user_id),
    )
    return cur.rowcount == 1


async def get_messages(conn: AsyncConnection, session_id: str, user_id: str) -> list[dict]:
    """Return all messages for a session, oldest first. Verifies ownership."""
    cur = await conn.execute(
        """
        SELECT m.id, m.role, m.content, m.created_at
        FROM genai.chat_messages m
        JOIN genai.chat_sessions s ON s.id = m.session_id
        WHERE m.session_id = %s AND s.user_id = %s
        ORDER BY m.created_at ASC
        """,
        (session_id, user_id),
    )
    rows = await cur.fetchall()
    return [
        {
            "id": str(r[0]),
            "role": r[1],
            "content": r[2],
            "created_at": r[3].isoformat(),
        }
        for r in rows
    ]


async def get_sessions(conn: AsyncConnection, user_id: str) -> list[dict]:
    """List all sessions for a user, newest first."""
    cur = await conn.execute(
        """
        SELECT
            s.id,
            s.session_type,
            s.summary,
            s.created_at,
            (
                SELECT content
                FROM genai.chat_messages
                WHERE session_id = s.id AND role = 'user'
                ORDER BY created_at ASC
                LIMIT 1
            ) AS first_message
        FROM genai.chat_sessions s
        WHERE s.user_id = %s
        ORDER BY s.created_at DESC
        """,
        (user_id,),
    )
    rows = await cur.fetchall()
    return [
        {
            "id": str(r[0]),
            "user_id": str(user_id),
            "session_type": r[1],
            "summary": r[2],
            "created_at": r[3].isoformat(),
            "first_message": r[4],
        }
        for r in rows
    ]
