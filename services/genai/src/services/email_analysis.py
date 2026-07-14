"""LLM analysis of one inbox email against a user's job applications.

Called by the internal email-analysis endpoint. Uses structured output so the
result is machine-applicable by the email service without any text parsing.
"""

import logging

from langchain_core.messages import HumanMessage, SystemMessage

from src.llm.client import llm
from src.models.email_analysis import (
    EVENT_TYPES,
    STAGES,
    EmailAnalysisRequest,
    EmailAnalysisResult,
)
from src.observability import trace_config
from src.prompts.email_analysis import EMAIL_ANALYSIS_SYSTEM_PROMPT

logger = logging.getLogger(__name__)

# Bodies are truncated defensively; the email service already truncates on its side.
_MAX_BODY_CHARS = 8000

_structured_llm = llm.with_structured_output(EmailAnalysisResult)


def _format_input(request: EmailAnalysisRequest) -> str:
    email = request.email
    lines = [
        "EMAIL",
        f"From: {email.sender or '(unknown)'}",
        f"Subject: {email.subject or '(no subject)'}",
        f"Received: {email.received_at or '(unknown)'}",
        "Body:",
        (email.body or "(no body)")[:_MAX_BODY_CHARS],
        "",
        "CANDIDATE APPLICATIONS",
    ]
    for app in request.applications:
        lines.append(
            f"- id={app.id} | company={app.company} | title={app.job_title} | stage={app.stage}"
        )
    return "\n".join(lines)


def _sanitize(result: EmailAnalysisResult, request: EmailAnalysisRequest) -> EmailAnalysisResult:
    """Never trust free-form LLM ids/enums — anything off-schema degrades to a no-op."""
    candidate_ids = {app.id for app in request.applications}
    if result.application_id not in candidate_ids:
        result.application_id = None
    if result.application_id is None:
        result.relevant = False
    if result.suggested_stage not in STAGES:
        result.suggested_stage = None
    if result.event is not None and result.event.event_type not in EVENT_TYPES:
        result.event.event_type = "email_received"
    if not result.relevant:
        result.suggested_stage = None
        result.event = None
        result.action_items = []
    return result


async def analyze_email(request: EmailAnalysisRequest) -> EmailAnalysisResult:
    result = await _structured_llm.ainvoke(
        [
            SystemMessage(content=EMAIL_ANALYSIS_SYSTEM_PROMPT),
            HumanMessage(content=_format_input(request)),
        ],
        config=trace_config(
            user_id=request.user_id,
            session_id=request.email.message_id,
            tags=["email-analysis"],
        ),
    )
    return _sanitize(result, request)
