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
    TimelineEvent,
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
    if not request.applications:
        lines.append("(none tracked yet)")
    for app in request.applications:
        lines.append(f"- id={app.id} | company={app.company} | title={app.job_title} | stage={app.stage}")
    return "\n".join(lines)


def _sanitize(result: EmailAnalysisResult, request: EmailAnalysisRequest) -> EmailAnalysisResult:
    """Never trust free-form LLM ids/enums — anything off-schema degrades to a no-op.

    A hallucinated application_id is nulled but keeps the email relevant (the
    unmatched branch can still auto-create); a relevant-but-unmatched result with
    no extracted company is unusable and degrades to not-relevant.
    """
    candidate_ids = {app.id for app in request.applications}
    if result.application_id not in candidate_ids:
        result.application_id = None
    if result.application_id is None and not (result.company or "").strip():
        result.relevant = False
    if result.suggested_stage not in STAGES:
        result.suggested_stage = None
    if result.event is not None and result.event.event_type not in EVENT_TYPES:
        result.event.event_type = "email_received"
    if result.relevant and result.event is None:
        # The downstream pipeline can only act on a relevant email through its event —
        # synthesize a generic one rather than silently dropping the email.
        result.event = TimelineEvent(
            event_type="email_received",
            title=f"Email from {result.company or 'the company'}",
            description="An email about this application was received.",
        )
    if not result.relevant:
        result.suggested_stage = None
        result.event = None
        result.action_items = []
        result.is_interview_invite = False
    return result


async def analyze_email(request: EmailAnalysisRequest) -> EmailAnalysisResult:
    raw = await _structured_llm.ainvoke(
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
    # with_structured_output is typed as returning dict | BaseModel; normalize for mypy
    # (and defensively, should the runtime ever hand back the dict form).
    result = raw if isinstance(raw, EmailAnalysisResult) else EmailAnalysisResult.model_validate(raw)
    return _sanitize(result, request)
