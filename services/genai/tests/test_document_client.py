import json

import httpx
import pytest


def _mock_async_client(monkeypatch: pytest.MonkeyPatch, handler):
    real_client = httpx.AsyncClient

    def factory(**kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return real_client(**kwargs)

    monkeypatch.setattr(httpx, "AsyncClient", factory)


@pytest.mark.asyncio
async def test_save_generated_document_posts_with_jwt(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.services.document_client import save_generated_document

    seen: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["auth"] = request.headers.get("Authorization")
        seen["url"] = str(request.url)
        seen["body"] = json.loads(request.content)
        return httpx.Response(201, json={"id": "d1", **seen["body"]})

    _mock_async_client(monkeypatch, handler)

    result = await save_generated_document("the-jwt", "app-1", "cover_letter", "Dear team")

    assert seen["auth"] == "Bearer the-jwt"
    assert seen["url"].endswith("/api/v1/documents")
    assert seen["body"] == {
        "application_id": "app-1",
        "type": "cover_letter",
        "content": "Dear team",
    }
    assert result["id"] == "d1"


@pytest.mark.asyncio
async def test_save_document_tool_rejects_bad_type(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.tools.documents import make_save_document_tool

    tool = make_save_document_tool("the-jwt")
    result = await tool.ainvoke({"application_id": "app-1", "document_type": "poem", "content": "x"})

    assert result.startswith("Not saved")


@pytest.mark.asyncio
async def test_save_document_tool_reports_failure_gracefully(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.tools.documents import make_save_document_tool

    _mock_async_client(monkeypatch, lambda request: httpx.Response(401, json={"code": "UNAUTHORIZED"}))

    tool = make_save_document_tool("bad-jwt")
    result = await tool.ainvoke({"application_id": "app-1", "document_type": "resume", "content": "cv"})

    assert result.startswith("Saving failed")


@pytest.mark.asyncio
async def test_save_document_tool_saves(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.tools.documents import make_save_document_tool

    _mock_async_client(monkeypatch, lambda request: httpx.Response(201, json={"id": "d1"}))

    tool = make_save_document_tool("the-jwt")
    result = await tool.ainvoke({"application_id": "app-1", "document_type": "resume", "content": "cv"})

    assert result == "Saved resume to application app-1."
