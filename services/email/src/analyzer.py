"""Application-detection pipeline: match stored emails to job applications.

For each pending email: fetch the user's applications (candidates), ask genai to
classify/match the email, and — when a confident match comes back — apply the
derived update (stage change + timeline event + action items) atomically via the
application service's internal API.

Failure semantics: transient errors (genai or application service down) leave the
email 'pending' and count an attempt; it is retried on the next poll cycle until
email_analysis_max_attempts, then marked 'failed'. Replays after partial failure
are safe — the application service dedupes on the Gmail message id.
"""
from __future__ import annotations

import logging

from sqlalchemy.orm import Session

from .clients import application_client, genai_client
from .config import get_settings
from .db import SessionLocal, list_pending_analysis, mark_analysis, record_analysis_failure

logger = logging.getLogger(__name__)
_settings = get_settings()


def analyze_pending() -> int:
    """Analyze up to a batch of pending emails. Returns how many reached a final state.

    Never raises: per-email failures are recorded and retried on later cycles, so one
    bad email (or a genai outage) cannot break the polling loop.
    """
    if not _settings.internal_service_token:
        logger.debug("Application detection disabled — INTERNAL_SERVICE_TOKEN not set")
        return 0

    db = SessionLocal()
    try:
        pending = list_pending_analysis(db, limit=_settings.email_analysis_batch_size)
        finalized = 0
        for email in pending:
            try:
                if _analyze_one(db, email):
                    finalized += 1
            except Exception:
                logger.exception(
                    "Analysis failed for message %s (user %s)",
                    email["message_id"],
                    email["user_id"],
                )
                record_analysis_failure(
                    db,
                    user_id=email["user_id"],
                    message_id=email["message_id"],
                    max_attempts=_settings.email_analysis_max_attempts,
                )
        return finalized
    finally:
        db.close()


def _analyze_one(db: Session, email: dict) -> bool:
    """Run one email through the pipeline. Returns True when a final state was reached."""
    user_id = email["user_id"]
    message_id = email["message_id"]

    candidates = application_client.list_applications(user_id)
    if not candidates:
        mark_analysis(db, user_id=user_id, message_id=message_id, status="irrelevant")
        return True

    verdict = genai_client.analyze_email(
        user_id=user_id,
        email={
            "message_id": message_id,
            "subject": email.get("subject"),
            "sender": email.get("sender"),
            "body": email.get("body") or email.get("snippet"),
            "received_at": email["received_at"].isoformat() if email.get("received_at") else None,
        },
        applications=[
            {
                "id": str(c["id"]),
                "company": c["company"],
                "job_title": c["jobTitle"],
                "stage": c["stage"],
            }
            for c in candidates
        ],
    )

    application_id = verdict.get("application_id")
    confident = (
        verdict.get("relevant")
        and application_id
        and verdict.get("confidence", 0.0) >= _settings.email_analysis_confidence_threshold
        and verdict.get("event")
    )
    if not confident:
        mark_analysis(db, user_id=user_id, message_id=message_id, status="irrelevant")
        return True

    event = verdict["event"]
    application_client.apply_email_update(
        application_id=application_id,
        user_id=user_id,
        source_message_id=message_id,
        suggested_stage=verdict.get("suggested_stage"),
        event={
            "eventType": event["event_type"],
            "title": event["title"],
            "description": event.get("description"),
            "occurredAt": email["received_at"].isoformat()
            if email.get("received_at")
            else None,
        },
        recommendations=[
            {"insight": item["insight"], "recommendedAction": item["recommended_action"]}
            for item in verdict.get("action_items", [])
        ],
    )
    mark_analysis(
        db,
        user_id=user_id,
        message_id=message_id,
        status="analyzed",
        matched_application_id=application_id,
    )
    logger.info(
        "Email %s matched application %s (user %s, stage suggestion: %s)",
        message_id,
        application_id,
        user_id,
        verdict.get("suggested_stage"),
    )
    return True
