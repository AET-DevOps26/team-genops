from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Session
# ---------------------------------------------------------------------------


class CreateSessionRequest(BaseModel):
    session_type: str = Field(
        default="insight_chat",
        pattern="^(insight_chat|cover_letter_chat|fit_analysis_chat|mock_interview)$",
    )
    # Required for mock_interview (the interview is about one job); optional otherwise, where
    # an application is bound later on first reference.
    application_id: str | None = None


class SessionResponse(BaseModel):
    id: str
    user_id: str
    session_type: str
    # The application this chat is about, bound on first reference. The UI needs it to know
    # whether a generated document has somewhere to be saved.
    application_id: str | None = None
    summary: str | None = None
    first_message: str | None = None
    # Mock-interview state; null for every other session type.
    interview_status: str | None = None
    interview_score: int | None = None
    created_at: str


class SessionListResponse(BaseModel):
    sessions: list[SessionResponse]


# ---------------------------------------------------------------------------
# Chat
# ---------------------------------------------------------------------------


class MessageRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=8000)


class InterviewCompetency(BaseModel):
    # Lenient on purpose: this is populated from LLM-produced JSON, and a single missing field
    # must not 500 a finished interview. Defaults keep the score card renderable regardless.
    name: str = ""
    score: int = 0
    comment: str | None = None


class InterviewResult(BaseModel):
    """Structured outcome of a finished mock interview — the score card."""

    score: int
    verdict: str | None = None
    summary: str | None = None
    competencies: list[InterviewCompetency] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    improvements: list[str] = Field(default_factory=list)
    ended_early: bool = False
    questions_answered: int = 0
    questions_total: int = 0


class MessageResponse(BaseModel):
    response: str
    # Present only on the turn that ends a mock interview — the final score card.
    interview: InterviewResult | None = None


class MessageItem(BaseModel):
    id: str
    role: str
    content: str
    created_at: str


class MessageListResponse(BaseModel):
    messages: list[MessageItem]
