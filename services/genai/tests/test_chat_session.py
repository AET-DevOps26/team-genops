"""
Chat session persistence.

Two behaviours carry security weight: a session is only ever reachable by its owner
(missing and not-owned are deliberately indistinguishable, so session ids cannot be
probed), and an application binds to a session only once.
"""

import pytest

from tests.conftest import FakeCursor, make_conn


@pytest.mark.asyncio
async def test_create_session_returns_metadata_with_no_application_bound():
    """An application is bound later, on first reference — never at creation."""
    from datetime import datetime

    from src.services.chat.session import create_session

    created = datetime(2026, 1, 1, 12, 0, 0)
    # RETURNING: id, user_id, session_type, application_id, interview_status, created_at
    conn = make_conn(results=[[("s-1", "u-1", "insight_chat", None, None, created)]])

    result = await create_session(conn, "u-1")

    assert result["id"] == "s-1"
    assert result["session_type"] == "insight_chat"
    assert result["application_id"] is None


@pytest.mark.asyncio
async def test_get_session_application_id_returns_the_bound_id():
    from src.services.chat.session import get_session_application_id

    conn = make_conn(results=[[("app-1",)]])

    assert await get_session_application_id(conn, "s-1") == "app-1"


@pytest.mark.asyncio
@pytest.mark.parametrize("rows", [[], [(None,)]])
async def test_get_session_application_id_is_none_for_a_general_chat(rows: list):
    from src.services.chat.session import get_session_application_id

    assert await get_session_application_id(make_conn(results=[rows]), "s-1") is None


@pytest.mark.asyncio
async def test_binding_an_application_only_applies_when_none_is_set():
    """
    A stray uuid later in the conversation must not silently re-target a session that
    is already about a specific job — the guard lives in the WHERE clause.
    """
    from src.services.chat.session import bind_session_application

    conn = make_conn()

    await bind_session_application(conn, "s-1", "app-1")

    sql = conn.sql_at(0)
    assert "UPDATE genai.chat_sessions" in sql
    assert "application_id IS NULL" in sql, "binding must be conditional on nothing being bound yet"
    assert conn.params_at(0) == ("app-1", "s-1")


@pytest.mark.asyncio
async def test_get_session_summary_strips_whitespace():
    from src.services.chat.session import get_session_summary

    assert await get_session_summary(make_conn(results=[[("  a summary  ",)]]), "s-1") == "a summary"


@pytest.mark.asyncio
@pytest.mark.parametrize("rows", [[], [(None,)], [("",)]])
async def test_get_session_summary_is_empty_until_the_first_segment(rows: list):
    from src.services.chat.session import get_session_summary

    assert await get_session_summary(make_conn(results=[rows]), "s-1") == ""


@pytest.mark.asyncio
async def test_delete_session_is_scoped_to_the_owner():
    from src.services.chat.session import delete_session

    conn = make_conn(results=[FakeCursor(rowcount=1)])

    assert await delete_session(conn, "s-1", "u-1") is True
    sql = conn.sql_at(0)
    assert "user_id = %s" in sql, "delete must be scoped by owner, not by session id alone"
    assert conn.params_at(0) == ("s-1", "u-1")


@pytest.mark.asyncio
async def test_delete_session_reports_false_when_nothing_matched():
    """Another user's session and a missing one are indistinguishable to the caller."""
    from src.services.chat.session import delete_session

    conn = make_conn(results=[FakeCursor(rowcount=0)])

    assert await delete_session(conn, "s-1", "u-1") is False


@pytest.mark.asyncio
async def test_get_messages_returns_none_when_the_session_is_not_owned():
    """
    None drives a 404 for both 'missing' and 'not yours', so a user cannot learn that
    someone else's session id exists.
    """
    from src.services.chat.session import get_messages

    conn = make_conn(results=[[]])  # ownership probe finds nothing

    assert await get_messages(conn, "s-1", "u-1") is None
    assert len(conn.calls) == 1, "must not query messages once ownership fails"


@pytest.mark.asyncio
async def test_get_messages_returns_them_oldest_first():
    from datetime import datetime

    from src.services.chat.session import get_messages

    ts = datetime(2026, 1, 1, 12, 0, 0)
    conn = make_conn(
        results=[
            [(1,)],  # ownership probe succeeds
            [("m-1", "user", "hi", ts), ("m-2", "assistant", "hello", ts)],
        ]
    )

    messages = await get_messages(conn, "s-1", "u-1")

    assert messages is not None, "an owned session must not read as missing"
    assert [m["role"] for m in messages] == ["user", "assistant"]
    assert messages[0]["content"] == "hi"
    assert "ORDER BY m.seq ASC" in conn.sql_at(1)


@pytest.mark.asyncio
async def test_get_sessions_is_scoped_to_the_user_and_newest_first():
    from datetime import datetime

    from src.services.chat.session import get_sessions

    ts = datetime(2026, 1, 1, 12, 0, 0)
    # id, session_type, summary, created_at, application_id, interview_status, interview_score, first_message
    conn = make_conn(results=[[("s-1", "insight_chat", "summary", ts, "app-1", None, None, "first question")]])

    sessions = await get_sessions(conn, "u-1")

    assert sessions[0]["id"] == "s-1"
    assert sessions[0]["application_id"] == "app-1"
    assert sessions[0]["first_message"] == "first question"
    sql = conn.sql_at(0)
    assert "WHERE s.user_id = %s" in sql
    assert "ORDER BY s.created_at DESC" in sql


@pytest.mark.asyncio
async def test_get_sessions_handles_an_unbound_session():
    from datetime import datetime

    from src.services.chat.session import get_sessions

    ts = datetime(2026, 1, 1, 12, 0, 0)
    conn = make_conn(results=[[("s-1", "insight_chat", None, ts, None, None, None, None)]])

    assert (await get_sessions(conn, "u-1"))[0]["application_id"] is None
