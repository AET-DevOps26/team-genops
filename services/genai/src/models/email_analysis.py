"""Request/response schemas for the internal email-analysis endpoint.

`EmailAnalysisResult` doubles as the LLM's structured-output schema
(`llm.with_structured_output`), so its docstrings/field descriptions are part of
the prompt — keep them instructive.
"""

from pydantic import BaseModel, Field

# `draft` exists in the public ApplicationStage enum but is never a valid
# email-suggested stage — an email always implies the application was submitted.
STAGES = ("applied", "follow_up", "interview", "offer", "closed")
EVENT_TYPES = (
    "stage_change",
    "email_received",
    "interview_scheduled",
    "offer_received",
    "rejection",
    "info_requested",
    "note",
)


class EmailPayload(BaseModel):
    message_id: str
    subject: str | None = None
    sender: str | None = None
    body: str | None = None
    received_at: str | None = None


class ApplicationCandidate(BaseModel):
    id: str
    company: str
    job_title: str
    stage: str


class EmailAnalysisRequest(BaseModel):
    user_id: str
    email: EmailPayload
    applications: list[ApplicationCandidate]


class TimelineEvent(BaseModel):
    """One timeline entry derived from the email."""

    event_type: str = Field(description="One of: stage_change, email_received, interview_scheduled, offer_received, rejection, info_requested, note")
    title: str = Field(description="Short label, e.g. 'Interview invitation from Acme'")
    description: str = Field(description="1-3 sentence factual summary of what the email says happened")


class ActionItem(BaseModel):
    """A next-best-action for the user derived from the email."""

    insight: str = Field(description="The observation the action is based on")
    recommended_action: str = Field(description="The concrete suggested next step")


class EmailAnalysisResult(BaseModel):
    """Verdict on one email: is it about a job application of the user's, and if so what changed."""

    relevant: bool = Field(
        description="True if this email is about a job application of the user's — whether or "
        "not it matches a candidate application. Newsletters, job-board digests, promotions "
        "and unrelated mail are not relevant."
    )
    application_id: str | None = Field(
        default=None,
        description="The id of the matched candidate application, or null if none matches.",
    )
    company: str | None = Field(
        default=None,
        description="Company the email is about, extracted from the email text. Always fill this for a relevant email, matched or not.",
    )
    position: str | None = Field(
        default=None,
        description="Job title/position the email is about, extracted from the email text, or null if not stated.",
    )
    is_interview_invite: bool = Field(
        default=False,
        description="True if the email invites the user to (or schedules) an interview.",
    )
    confidence: float = Field(ge=0.0, le=1.0, description="Confidence in the match and interpretation, 0.0-1.0.")
    suggested_stage: str | None = Field(
        default=None,
        description="New stage if the email clearly implies a FORWARD transition (applied → follow_up → interview → offer → closed), else null.",
    )
    event: TimelineEvent | None = Field(default=None, description="Timeline entry for the email; null if not relevant.")
    action_items: list[ActionItem] = Field(
        default_factory=list,
        description="Concrete next steps for the user (follow up, send requested documents, prepare for a scheduled interview). Empty if none.",
    )
