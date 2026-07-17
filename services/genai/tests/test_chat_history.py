"""
Message history.

All messages are persisted; only a bounded window is replayed to the LLM. What falls out
of that window is covered by the rolling summary, so the window size is a cost decision
rather than a correctness one — but the mapping back to LangChain message types and the
oldest-first ordering are correctness.
"""

import pytest
from langchain_core.messages import AIMessage, HumanMessage

from tests.conftest import make_conn


@pytest.mark.asyncio
async def test_history_is_bounded_by_the_replay_window():
    from src.services.chat.utils.history import HISTORY_WINDOW, load_history

    conn = make_conn(results=[[]])

    await load_history(conn, "s-1")

    assert conn.params_at(0) == ("s-1", HISTORY_WINDOW)


@pytest.mark.asyncio
async def test_history_is_returned_oldest_first():
    """The rows come back newest-first from the inner query and are re-ordered for the LLM."""
    from src.services.chat.utils.history import load_history

    conn = make_conn(results=[[("user", "first"), ("assistant", "second")]])

    messages = await load_history(conn, "s-1")

    assert [m.content for m in messages] == ["first", "second"]
    assert "ORDER BY seq ASC" in conn.sql_at(0)


@pytest.mark.asyncio
async def test_roles_map_to_langchain_message_types():
    from src.services.chat.utils.history import load_history

    conn = make_conn(results=[[("user", "hi"), ("assistant", "hello")]])

    messages = await load_history(conn, "s-1")

    assert isinstance(messages[0], HumanMessage)
    assert isinstance(messages[1], AIMessage)


@pytest.mark.asyncio
async def test_an_empty_session_has_no_history():
    from src.services.chat.utils.history import load_history

    assert await load_history(make_conn(results=[[]]), "s-1") == []


@pytest.mark.asyncio
async def test_save_message_persists_role_and_content():
    from src.services.chat.utils.history import save_message

    conn = make_conn()

    await save_message(conn, "s-1", "user", "hi")

    assert "INSERT INTO genai.chat_messages" in conn.sql_at(0)
    assert conn.params_at(0) == ("s-1", "user", "hi")


@pytest.mark.asyncio
async def test_count_messages_returns_the_total():
    from src.services.chat.utils.history import count_messages

    assert await count_messages(make_conn(results=[[(7,)]]), "s-1") == 7


@pytest.mark.asyncio
async def test_transcript_is_labelled_for_the_summarizer():
    from src.services.chat.utils.history import load_last_n_messages_as_text

    conn = make_conn(results=[[("user", "hi"), ("assistant", "hello"), ("user", "bye")]])

    text = await load_last_n_messages_as_text(conn, "s-1", 10)

    assert text == "User: hi\nAssistant: hello\nUser: bye"


@pytest.mark.asyncio
async def test_transcript_honours_the_requested_size():
    """The caller passes SUMMARY_EVERY here, not the replay window — they differ."""
    from src.services.chat.utils.history import load_last_n_messages_as_text

    conn = make_conn(results=[[]])

    await load_last_n_messages_as_text(conn, "s-1", 10)

    assert conn.params_at(0) == ("s-1", 10)


@pytest.mark.asyncio
async def test_an_empty_transcript_is_an_empty_string():
    from src.services.chat.utils.history import load_last_n_messages_as_text

    assert await load_last_n_messages_as_text(make_conn(results=[[]]), "s-1", 10) == ""
