import httpx
import pytest


@pytest.mark.asyncio
async def test_health(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.main import app

    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
