"""
Unit tests for the LangChain tool factories (src/tools/).

The underlying service/RAG calls are monkeypatched, so these assert that the
tools forward the right arguments — the user's JWT, the application id, the
query plus the session/user scoping — without any network or DB access.
"""

import pytest

from src.tools import applications as applications_tools
from src.tools import session_memory as session_memory_tools
from tests.conftest import make_conn


@pytest.mark.asyncio
async def test_list_job_applications_forwards_token(monkeypatch: pytest.MonkeyPatch):
    seen = {}

    async def fake_list(token: str) -> str:
        seen["token"] = token
        return "app list"

    monkeypatch.setattr(applications_tools, "_list_applications", fake_list)

    tools = applications_tools.make_application_tools("jwt-abc")
    list_tool = next(t for t in tools if t.name == "list_job_applications")

    result = await list_tool.ainvoke({})

    assert result == "app list"
    assert seen["token"] == "jwt-abc"


@pytest.mark.asyncio
async def test_get_job_application_forwards_token_and_id(monkeypatch: pytest.MonkeyPatch):
    seen = {}

    async def fake_get(token: str, application_id: str) -> str:
        seen["token"] = token
        seen["id"] = application_id
        return "app detail"

    monkeypatch.setattr(applications_tools, "_get_application", fake_get)

    tools = applications_tools.make_application_tools("jwt-abc")
    get_tool = next(t for t in tools if t.name == "get_job_application")

    result = await get_tool.ainvoke({"application_id": "app-42"})

    assert result == "app detail"
    assert seen == {"token": "jwt-abc", "id": "app-42"}


@pytest.mark.asyncio
async def test_session_memory_tool_scopes_search(monkeypatch: pytest.MonkeyPatch):
    seen: dict[str, str] = {}

    async def fake_search(conn, user_id, query, session_id):
        seen.update(user_id=user_id, query=query, session_id=session_id)
        return "past context"

    monkeypatch.setattr(session_memory_tools, "_search", fake_search)

    conn = make_conn()
    tool = session_memory_tools.make_session_memory_tool(conn, "user-1", "sess-1")

    result = await tool.ainvoke({"query": "what did we discuss about Acme"})

    assert result == "past context"
    assert seen == {
        "user_id": "user-1",
        "query": "what did we discuss about Acme",
        "session_id": "sess-1",
    }
