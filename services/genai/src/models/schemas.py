from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Session
# ---------------------------------------------------------------------------

class CreateSessionRequest(BaseModel):
    session_type: str = Field(
        default="insight_chat",
        pattern="^(insight_chat|cover_letter_chat|fit_analysis_chat)$",
    )


class SessionResponse(BaseModel):
    id: str
    user_id: str
    session_type: str
    summary: str | None = None
    first_message: str | None = None
    created_at: str


class SessionListResponse(BaseModel):
    sessions: list[SessionResponse]


# ---------------------------------------------------------------------------
# Chat
# ---------------------------------------------------------------------------

class MessageRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=8000)


class MessageResponse(BaseModel):
    response: str


class MessageItem(BaseModel):
    id: str
    role: str
    content: str
    created_at: str


class MessageListResponse(BaseModel):
    messages: list[MessageItem]


# ---------------------------------------------------------------------------
# Job postings — AI extraction
# ---------------------------------------------------------------------------

class JobPostingExtractRequest(BaseModel):
    # Kept as a plain string so URL problems surface as the contract's
    # `URL_INVALID` error body instead of FastAPI's default 422 shape.
    url: str = Field(..., min_length=1, max_length=2048)


class JobPostingExtraction(BaseModel):
    """Fields extracted from a job posting. Null where the page lacked the info."""

    company: str | None = None
    job_title: str | None = None
    job_description: str | None = None
