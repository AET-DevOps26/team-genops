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
    # The application this chat is about, bound on first reference. The UI needs it to know
    # whether a generated document has somewhere to be saved.
    application_id: str | None = None
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
