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
        "applications": applications
        if applications is not None
        else [{"id": "app-1", "company": "Acme", "job_title": "Engineer", "stage": "applied"}],
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
        response = await c.post(
            "/internal/v1/email-analysis", json=_request_body(), headers={"Authorization": ""}
        )
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
async def test_no_candidates_short_circuits_without_llm(client, monkeypatch):
    called = False

    async def fake_analyze(request):
        nonlocal called
        called = True

    import src.routers.internal_analysis as router_module

    monkeypatch.setattr(router_module, "analyze_email", fake_analyze)
    async with client as c:
        response = await c.post("/internal/v1/email-analysis", json=_request_body(applications=[]))

    assert response.status_code == 200
    assert response.json()["relevant"] is False
    assert called is False


@pytest.mark.asyncio
async def test_analysis_result_passthrough(client, monkeypatch):
    from src.models.email_analysis import EmailAnalysisResult, TimelineEvent

    async def fake_analyze(request):
        return EmailAnalysisResult(
            relevant=True,
            application_id="app-1",
            confidence=0.9,
            suggested_stage="interview",
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
    assert body["event"]["event_type"] == "interview_scheduled"


def test_sanitize_rejects_unknown_ids_and_enums(monkeypatch):
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
        suggested_stage="hired",  # not a real stage
        event=TimelineEvent(event_type="party", title="t", description="d"),
    )

    sanitized = _sanitize(result, request)

    # Unknown id → treated as no-match → not relevant, nothing to apply.
    assert sanitized.relevant is False
    assert sanitized.application_id is None
    assert sanitized.suggested_stage is None
    assert sanitized.event is None
    assert sanitized.action_items == []


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
        confidence=0.8,
        event=TimelineEvent(event_type="party", title="t", description="d"),
    )

    sanitized = _sanitize(result, request)

    assert sanitized.relevant is True
    assert sanitized.event.event_type == "email_received"
