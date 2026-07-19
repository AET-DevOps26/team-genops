"""Tests for the internal email-analysis endpoint and result sanitization."""

import httpx
import pytest


def _request_body(applications: list[dict] | None = None) -> dict:
    return {
        "user_id": "11111111-1111-1111-1111-111111111111",
        "email": {
            "message_id": "gmail-msg-1",
            "subject": "Interview invitation",
            "sender": "recruiting@acme.example",
            "body": "We would like to invite you to an interview on Friday.",
            "received_at": "2026-07-14T10:00:00Z",
        },
        "applications": applications if applications is not None else [{"id": "app-1", "company": "Acme", "job_title": "Engineer", "stage": "applied"}],
    }


@pytest.fixture()
def client(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.config import settings
    from src.main import app

    monkeypatch.setattr(settings, "internal_service_token", "test-internal-token")
    transport = httpx.ASGITransport(app=app)
    return httpx.AsyncClient(
        transport=transport,
        base_url="http://test",
        headers={"Authorization": "Bearer test-internal-token"},
    )


@pytest.mark.asyncio
async def test_missing_token_rejected(client, monkeypatch):
    async with client as c:
        response = await c.post("/internal/v1/email-analysis", json=_request_body(), headers={"Authorization": ""})
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_wrong_token_rejected(client):
    async with client as c:
        response = await c.post(
            "/internal/v1/email-analysis",
            json=_request_body(),
            headers={"Authorization": "Bearer wrong"},
        )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_blank_configured_token_fails_closed(client, monkeypatch):
    from src.config import settings

    monkeypatch.setattr(settings, "internal_service_token", "")
    async with client as c:
        response = await c.post("/internal/v1/email-analysis", json=_request_body())
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_empty_candidates_still_calls_llm(client, monkeypatch):
    """No tracked applications is the auto-create path — the LLM must still run."""
    from src.models.email_analysis import EmailAnalysisResult

    called = False

    async def fake_analyze(request):
        nonlocal called
        called = True
        return EmailAnalysisResult(relevant=True, company="Acme", position="Engineer", confidence=0.9)

    import src.routers.internal_analysis as router_module

    monkeypatch.setattr(router_module, "analyze_email", fake_analyze)
    async with client as c:
        response = await c.post("/internal/v1/email-analysis", json=_request_body(applications=[]))

    assert response.status_code == 200
    assert called is True
    body = response.json()
    assert body["relevant"] is True
    assert body["company"] == "Acme"


@pytest.mark.asyncio
async def test_analysis_result_passthrough(client, monkeypatch):
    from src.models.email_analysis import EmailAnalysisResult, TimelineEvent

    async def fake_analyze(request):
        return EmailAnalysisResult(
            relevant=True,
            application_id="app-1",
            company="Acme",
            confidence=0.9,
            suggested_stage="interview",
            is_interview_invite=True,
            event=TimelineEvent(
                event_type="interview_scheduled",
                title="Interview invitation",
                description="Acme invited you to interview on Friday.",
            ),
        )

    import src.routers.internal_analysis as router_module

    monkeypatch.setattr(router_module, "analyze_email", fake_analyze)
    async with client as c:
        response = await c.post("/internal/v1/email-analysis", json=_request_body())

    assert response.status_code == 200
    body = response.json()
    assert body["application_id"] == "app-1"
    assert body["suggested_stage"] == "interview"
    assert body["is_interview_invite"] is True
    assert body["event"]["event_type"] == "interview_scheduled"


def test_sanitize_nulls_hallucinated_id_but_keeps_relevance(monkeypatch):
    """A hallucinated id degrades to the unmatched branch when a company was extracted."""
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.models.email_analysis import (
        EmailAnalysisRequest,
        EmailAnalysisResult,
        TimelineEvent,
    )
    from src.services.email_analysis import _sanitize

    request = EmailAnalysisRequest.model_validate(_request_body())
    result = EmailAnalysisResult(
        relevant=True,
        application_id="hallucinated-id",
        company="Globex",
        position="Engineer",
        confidence=0.9,
        suggested_stage="hired",  # not a real stage
        event=TimelineEvent(event_type="party", title="t", description="d"),
    )

    sanitized = _sanitize(result, request)

    assert sanitized.relevant is True
    assert sanitized.application_id is None
    assert sanitized.company == "Globex"
    assert sanitized.suggested_stage is None
    assert sanitized.event is not None
    assert sanitized.event.event_type == "email_received"


def test_sanitize_degrades_unmatched_without_company(monkeypatch):
    """Unknown id and no extracted company → unusable → not relevant, nothing to apply."""
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.models.email_analysis import (
        EmailAnalysisRequest,
        EmailAnalysisResult,
        TimelineEvent,
    )
    from src.services.email_analysis import _sanitize

    request = EmailAnalysisRequest.model_validate(_request_body())
    result = EmailAnalysisResult(
        relevant=True,
        application_id="hallucinated-id",
        confidence=0.9,
        suggested_stage="interview",
        is_interview_invite=True,
        event=TimelineEvent(event_type="interview_scheduled", title="t", description="d"),
    )

    sanitized = _sanitize(result, request)

    assert sanitized.relevant is False
    assert sanitized.application_id is None
    assert sanitized.suggested_stage is None
    assert sanitized.event is None
    assert sanitized.action_items == []
    assert sanitized.is_interview_invite is False


def test_sanitize_synthesizes_event_for_relevant_result_without_one(monkeypatch):
    """A relevant verdict is only actionable through its event — one is synthesized if missing."""
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.models.email_analysis import EmailAnalysisRequest, EmailAnalysisResult
    from src.services.email_analysis import _sanitize

    request = EmailAnalysisRequest.model_validate(_request_body())
    result = EmailAnalysisResult(relevant=True, application_id="app-1", company="Acme", confidence=0.9)

    sanitized = _sanitize(result, request)

    assert sanitized.relevant is True
    assert sanitized.event is not None
    assert sanitized.event.event_type == "email_received"
    assert "Acme" in sanitized.event.title


def test_sanitize_rejects_draft_stage(monkeypatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.models.email_analysis import EmailAnalysisRequest, EmailAnalysisResult
    from src.services.email_analysis import _sanitize

    request = EmailAnalysisRequest.model_validate(_request_body())
    result = EmailAnalysisResult(
        relevant=True,
        application_id="app-1",
        company="Acme",
        confidence=0.8,
        suggested_stage="draft",
    )

    sanitized = _sanitize(result, request)

    assert sanitized.suggested_stage is None


def test_sanitize_downgrades_unknown_event_type(monkeypatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")

    from src.models.email_analysis import (
        EmailAnalysisRequest,
        EmailAnalysisResult,
        TimelineEvent,
    )
    from src.services.email_analysis import _sanitize

    request = EmailAnalysisRequest.model_validate(_request_body())
    result = EmailAnalysisResult(
        relevant=True,
        application_id="app-1",
        company="Acme",
        confidence=0.8,
        event=TimelineEvent(event_type="party", title="t", description="d"),
    )

    sanitized = _sanitize(result, request)

    assert sanitized.relevant is True
    assert sanitized.event is not None
    assert sanitized.event.event_type == "email_received"
