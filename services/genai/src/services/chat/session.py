from psycopg import AsyncConnection
from psycopg.types.json import Jsonb


async def create_session(
    conn: AsyncConnection,
    user_id: str,
    session_type: str = "insight_chat",
    application_id: str | None = None,
) -> dict:
    """
    Create a new chat session and return its metadata.

    `application_id` is bound at creation for a mock_interview (the whole interview is about
    one job, verified by the caller before we get here); for other session types it is
    normally None and bound later on first reference. A mock_interview also starts its
    lifecycle in 'in_progress' — every other type leaves interview_status NULL.
    """
    interview_status = "in_progress" if session_type == "mock_interview" else None
    cur = await conn.execute(
        """
        INSERT INTO genai.chat_sessions (user_id, session_type, application_id, interview_status)
        VALUES (%s, %s, %s, %s)
        RETURNING id, user_id, session_type, application_id, interview_status, created_at
        """,
        (user_id, session_type, application_id, interview_status),
    )
    row = await cur.fetchone()
    assert row is not None  # INSERT ... RETURNING always yields a row
    return {
        "id": str(row[0]),
        "user_id": str(row[1]),
        "session_type": row[2],
        "application_id": str(row[3]) if row[3] else None,
        "interview_status": row[4],
        "created_at": row[5].isoformat(),
    }


async def get_session_meta(conn: AsyncConnection, session_id: str, user_id: str) -> dict | None:
    """
    Return session type + interview state for an owned session, or None if missing/not owned.

    The interview flow needs to know which mode a session is in and where the interview
    stands before it acts; folding the ownership check in here keeps that a single query.
    """
    cur = await conn.execute(
        """
        SELECT session_type, application_id, interview_status, interview_score
        FROM genai.chat_sessions
        WHERE id = %s AND user_id = %s
        """,
        (session_id, user_id),
    )
    row = await cur.fetchone()
    if not row:
        return None
    return {
        "session_type": row[0],
        "application_id": str(row[1]) if row[1] else None,
        "interview_status": row[2],
        "interview_score": row[3],
    }


async def complete_interview(conn: AsyncConnection, session_id: str, score: int, evaluation: dict) -> None:
    """Persist the final score + structured evaluation and mark the interview completed."""
    await conn.execute(
        """
        UPDATE genai.chat_sessions
        SET interview_status = 'completed',
            interview_score = %s,
            interview_evaluation = %s,
            updated_at = NOW()
        WHERE id = %s
        """,
        (score, Jsonb(evaluation), session_id),
    )


async def get_session_application_id(conn: AsyncConnection, session_id: str) -> str | None:
    """Return the application this session is about, or None if it is a general chat."""
    cur = await conn.execute(
        "SELECT application_id FROM genai.chat_sessions WHERE id = %s",
        (session_id,),
    )
    row = await cur.fetchone()
    return str(row[0]) if row and row[0] else None


async def bind_session_application(conn: AsyncConnection, session_id: str, application_id: str) -> None:
    """
    Attach an application to a session the first time one is referenced.

    The id arrives in a single message ("...application id: <uuid>"), but the job context
    is needed on every later turn too — "make it shorter" must still know which job. Binding
    it to the session is what makes the context outlive that one message.

    Only sets it when unset, so a stray id later in the conversation cannot silently
    re-target a session that is already about a specific job.
    """
    await conn.execute(
        """
        UPDATE genai.chat_sessions
        SET application_id = %s, updated_at = NOW()
        WHERE id = %s AND application_id IS NULL
        """,
        (application_id, session_id),
    )


async def get_session_summary(conn: AsyncConnection, session_id: str) -> str:
    """
    Return the session's rolling summary — the compressed record of the messages that have
    already scrolled out of the verbatim history window. Empty string until the first
    segment is summarized.
    """
    cur = await conn.execute(
        "SELECT summary FROM genai.chat_sessions WHERE id = %s",
        (session_id,),
    )
    row = await cur.fetchone()
    return (row[0] or "").strip() if row else ""


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


async def get_messages(conn: AsyncConnection, session_id: str, user_id: str) -> list[dict] | None:
    """Return all messages for a session, oldest first. Returns None if not found/not owned."""
    # Verify session exists and belongs to user (return None for both cases → 404)
    cur = await conn.execute(
        "SELECT 1 FROM genai.chat_sessions WHERE id = %s AND user_id = %s",
        (session_id, user_id),
    )
    if not await cur.fetchone():
        return None

    cur = await conn.execute(
        """
        SELECT m.id, m.role, m.content, m.created_at
        FROM genai.chat_messages m
        WHERE m.session_id = %s
        ORDER BY m.seq ASC
        """,
        (session_id,),
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
            s.application_id,
            s.interview_status,
            s.interview_score,
            (
                SELECT content
                FROM genai.chat_messages
                WHERE session_id = s.id AND role = 'user'
                ORDER BY seq ASC
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
            "application_id": str(r[4]) if r[4] else None,
            "interview_status": r[5],
            "interview_score": r[6],
            "first_message": r[7],
        }
        for r in rows
    ]
