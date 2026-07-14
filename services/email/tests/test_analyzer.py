"""Tests for the application-detection analyzer (genai + application service faked)."""
from __future__ import annotations

from datetime import datetime, timezone

import pytest

from src import analyzer


RECEIVED = datetime(2026, 7, 14, 10, 0, tzinfo=timezone.utc)


def _email(message_id: str = "m1") -> dict:
    return {
        "user_id": "u1",
        "message_id": message_id,
        "subject": "Interview invitation",
        "sender": "recruiting@acme.example",
        "snippet": "snippet",
        "body": "We would like to invite you to an interview.",
        "received_at": RECEIVED,
        "analysis_attempts": 0,
    }


def _candidate() -> dict:
    return {"id": "app-1", "company": "Acme", "jobTitle": "Engineer", "stage": "applied"}


def _verdict(**overrides) -> dict:
    verdict = {
        "relevant": True,
        "application_id": "app-1",
        "confidence": 0.9,
        "suggested_stage": "interview",
        "event": {
            "event_type": "interview_scheduled",
            "title": "Interview invitation",
            "description": "Acme invited you.",
        },
        "action_items": [
            {"insight": "Interview on Friday", "recommended_action": "Prepare"}
        ],
    }
    verdict.update(overrides)
    return verdict


class Recorder:
    def __init__(self):
        self.marked: list[tuple[str, str, str | None]] = []
        self.failures: list[str] = []
        self.applied: list[dict] = []


@pytest.fixture()
def recorder(monkeypatch: pytest.MonkeyPatch) -> Recorder:
    rec = Recorder()
    monkeypatch.setattr(analyzer._settings, "internal_service_token", "token")
    monkeypatch.setattr(analyzer, "SessionLocal", lambda: _FakeSession())
    monkeypatch.setattr(
        analyzer,
        "mark_analysis",
        lambda db, *, user_id, message_id, status, matched_application_id=None: rec.marked.append(
            (message_id, status, matched_application_id)
        ),
    )
    monkeypatch.setattr(
        analyzer,
        "record_analysis_failure",
        lambda db, *, user_id, message_id, max_attempts: rec.failures.append(message_id),
    )
    monkeypatch.setattr(
        analyzer.application_client,
        "apply_email_update",
        lambda **kwargs: rec.applied.append(kwargs) or True,
    )
    return rec


class _FakeSession:
    def close(self):
        pass


def _set_pending(monkeypatch, emails):
    monkeypatch.setattr(analyzer, "list_pending_analysis", lambda db, *, limit: emails)


def test_happy_path_applies_update(monkeypatch, recorder):
    _set_pending(monkeypatch, [_email()])
    monkeypatch.setattr(analyzer.application_client, "list_applications", lambda uid: [_candidate()])
    monkeypatch.setattr(analyzer.genai_client, "analyze_email", lambda **kw: _verdict())

    assert analyzer.analyze_pending() == 1
    assert recorder.marked == [("m1", "analyzed", "app-1")]
    assert len(recorder.applied) == 1
    applied = recorder.applied[0]
    assert applied["application_id"] == "app-1"
    assert applied["suggested_stage"] == "interview"
    assert applied["event"]["eventType"] == "interview_scheduled"
    assert applied["event"]["occurredAt"] == RECEIVED.isoformat()
    assert applied["recommendations"] == [
        {"insight": "Interview on Friday", "recommendedAction": "Prepare"}
    ]


def test_irrelevant_email_marked_without_update(monkeypatch, recorder):
    _set_pending(monkeypatch, [_email()])
    monkeypatch.setattr(analyzer.application_client, "list_applications", lambda uid: [_candidate()])
    monkeypatch.setattr(
        analyzer.genai_client,
        "analyze_email",
        lambda **kw: _verdict(relevant=False, application_id=None, event=None),
    )

    assert analyzer.analyze_pending() == 1
    assert recorder.marked == [("m1", "irrelevant", None)]
    assert recorder.applied == []


def test_low_confidence_marked_irrelevant(monkeypatch, recorder):
    _set_pending(monkeypatch, [_email()])
    monkeypatch.setattr(analyzer.application_client, "list_applications", lambda uid: [_candidate()])
    monkeypatch.setattr(analyzer.genai_client, "analyze_email", lambda **kw: _verdict(confidence=0.3))

    assert analyzer.analyze_pending() == 1
    assert recorder.marked == [("m1", "irrelevant", None)]
    assert recorder.applied == []


def test_no_applications_skips_llm(monkeypatch, recorder):
    _set_pending(monkeypatch, [_email()])
    monkeypatch.setattr(analyzer.application_client, "list_applications", lambda uid: [])

    def boom(**kw):
        raise AssertionError("genai must not be called without candidates")

    monkeypatch.setattr(analyzer.genai_client, "analyze_email", boom)

    assert analyzer.analyze_pending() == 1
    assert recorder.marked == [("m1", "irrelevant", None)]


def test_genai_error_records_failure_and_continues(monkeypatch, recorder):
    _set_pending(monkeypatch, [_email("m1"), _email("m2")])
    monkeypatch.setattr(analyzer.application_client, "list_applications", lambda uid: [_candidate()])

    calls = iter([RuntimeError("genai down"), _verdict()])

    def flaky(**kw):
        item = next(calls)
        if isinstance(item, Exception):
            raise item
        return item

    monkeypatch.setattr(analyzer.genai_client, "analyze_email", flaky)

    assert analyzer.analyze_pending() == 1
    assert recorder.failures == ["m1"]
    assert recorder.marked == [("m2", "analyzed", "app-1")]


def test_disabled_without_token(monkeypatch, recorder):
    monkeypatch.setattr(analyzer._settings, "internal_service_token", "")

    def boom(db, *, limit):
        raise AssertionError("must not query when disabled")

    monkeypatch.setattr(analyzer, "list_pending_analysis", boom)
    assert analyzer.analyze_pending() == 0
