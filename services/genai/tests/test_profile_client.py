import httpx
import pytest

AGGREGATE = {
    "profile": {
        "first_name": "Jane",
        "last_name": "Doe",
        "bio": "CS student",
        "location": "Munich",
        "website": "https://jane.dev",
    },
    "work_experiences": [
        {
            "company": "Acme",
            "role": "Engineer",
            "location": "Berlin",
            "start_date": "2024-03-01",
            "end_date": None,
            "is_current": True,
            "description": "Built things",
        }
    ],
    "educations": [
        {
            "institution": "TUM",
            "degree": "MSc",
            "field": "Computer Science",
            "start_date": "2025-10-01",
            "end_date": None,
        }
    ],
    "skills": [{"name": "Java", "level": "advanced"}],
    "languages": [{"name": "German", "proficiency": "fluent"}],
}


def _mock_async_client(monkeypatch: pytest.MonkeyPatch, handler):
    """Route every httpx.AsyncClient request in the module under test to `handler`."""
    real_client = httpx.AsyncClient

    def factory(**kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return real_client(**kwargs)

    monkeypatch.setattr(httpx, "AsyncClient", factory)


def test_format_profile_renders_all_sections(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.services.profile_client import _format_profile

    text = _format_profile(AGGREGATE)

    assert "Name: Jane Doe" in text
    assert "Location: Munich" in text
    assert "- Engineer at Acme (2024-03-01 – present), Berlin" in text
    assert "Built things" in text
    assert "- MSc in Computer Science, TUM" in text
    assert "Skills: Java (advanced)" in text
    assert "Languages: German (fluent)" in text


def test_format_profile_minimal(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.services.profile_client import _format_profile

    text = _format_profile(
        {
            "profile": {"first_name": "Jane", "last_name": "Doe"},
            "work_experiences": [],
            "educations": [],
            "skills": [],
            "languages": [],
        }
    )

    assert text == "User Profile:\nName: Jane Doe"


@pytest.mark.asyncio
async def test_get_user_profile_disabled(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.config import settings
    from src.services.profile_client import _NOT_ENABLED, get_user_profile

    monkeypatch.setattr(settings, "profile_enabled", False)

    assert await get_user_profile("token") == _NOT_ENABLED


@pytest.mark.asyncio
async def test_get_user_profile_forwards_jwt_and_formats(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.config import settings
    from src.services.profile_client import get_user_profile

    monkeypatch.setattr(settings, "profile_enabled", True)
    seen: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["auth"] = request.headers.get("Authorization")
        seen["url"] = str(request.url)
        return httpx.Response(200, json=AGGREGATE)

    _mock_async_client(monkeypatch, handler)

    text = await get_user_profile("the-jwt")

    assert seen["auth"] == "Bearer the-jwt"
    assert seen["url"].endswith("/api/v1/profile")
    assert "Name: Jane Doe" in text


@pytest.mark.asyncio
async def test_get_user_profile_404_means_no_profile_yet(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.config import settings
    from src.services.profile_client import _NO_PROFILE, get_user_profile

    monkeypatch.setattr(settings, "profile_enabled", True)
    _mock_async_client(monkeypatch, lambda request: httpx.Response(404, json={"code": "PROFILE_NOT_FOUND"}))

    assert await get_user_profile("the-jwt") == _NO_PROFILE
