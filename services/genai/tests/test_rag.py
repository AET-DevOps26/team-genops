"""
Unit tests for the RAG past-session search (src/services/chat/utils/rag.py).

The embedding call is monkeypatched (no model access) and the DB is the shared
FakeConn double, so these assert the query shape, the pgvector Vector binding,
and the formatting of retrieved summaries.
"""

import pytest

from src.services.chat.utils import rag
from tests.conftest import make_conn


@pytest.fixture(autouse=True)
def _stub_embedding(monkeypatch: pytest.MonkeyPatch):
    async def fake_embed(text: str) -> list[float]:
        return [0.1, 0.2, 0.3]

    monkeypatch.setattr(rag, "embed_text", fake_embed)


@pytest.mark.asyncio
async def test_returns_placeholder_when_no_matches():
    conn = make_conn([[]])  # one execute → empty result set

    result = await rag.search_past_sessions(conn, "user-1", "tell me about Acme", "sess-1")

    assert result == "No relevant past sessions found."


@pytest.mark.asyncio
async def test_formats_each_retrieved_summary():
    conn = make_conn([[("Talked about Acme interview.",), ("Reviewed the Zalando offer.",)]])

    result = await rag.search_past_sessions(conn, "user-1", "offers", "sess-1")

    assert "Past session 1:\nTalked about Acme interview." in result
    assert "Past session 2:\nReviewed the Zalando offer." in result


@pytest.mark.asyncio
async def test_excludes_current_session_and_binds_vector():
    conn = make_conn([[("summary",)]])

    await rag.search_past_sessions(conn, "user-1", "q", "current-sess")

    sql = conn.sql_at(0)
    assert "id != %s" in sql
    assert "embedding <=> %s" in sql
    params = conn.params_at(0)
    # (user_id, current_session_id, query_embedding, TOP_K)
    assert params[0] == "user-1"
    assert params[1] == "current-sess"
    assert params[3] == rag.TOP_K
    # The embedding must be bound as a pgvector Vector, never a bare list.
    from pgvector import Vector

    assert isinstance(params[2], Vector)
