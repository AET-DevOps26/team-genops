from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from psycopg import AsyncConnection

from src.auth import get_access_token, get_current_user_id
from src.db.pool import get_conn
from src.models.schemas import (
    CreateSessionRequest,
    InterviewResult,
    MessageItem,
    MessageListResponse,
    MessageRequest,
    MessageResponse,
    SessionListResponse,
    SessionResponse,
)
from src.services.application_client import application_has_job_description, fetch_application
from src.services.chat import chat, create_session, delete_session, get_messages, get_sessions, interview
from src.services.chat.session import get_session_meta
from src.services.chat.utils.history import count_messages
from src.services.chat.utils.summarizer import maybe_summarize
from src.services.profile_client import fetch_profile, profile_is_complete

router = APIRouter(prefix="/api/v1/chat", tags=["Chat"])


async def _require_interview_ready(token: str, application_id: str | None) -> str:
    """
    Gate a mock interview on its two hard preconditions: a bound application that carries a
    job description, and a complete-enough candidate profile. Returns the validated
    application id, or raises 422 with a message the UI can show directly.
    """
    if not application_id:
        raise HTTPException(status_code=422, detail="A mock interview must be started from a job application.")

    application = await fetch_application(token, application_id)
    if application is None:
        raise HTTPException(status_code=422, detail="That job application could not be found.")
    if not application_has_job_description(application):
        raise HTTPException(
            status_code=422,
            detail="This application has no job description yet — add one so the interview can be tailored to the role.",
        )

    profile = await fetch_profile(token)
    if profile is None or not profile_is_complete(profile):
        raise HTTPException(
            status_code=422,
            detail="Complete your profile (name plus experience, education, or skills) before starting an interview.",
        )
    return application_id


async def _background_summarize(session_id: str, message_count: int, transcript: str) -> None:
    """
    Runs summarization in the background using its own DB connection.
    message_count and transcript are captured at trigger time to ensure we summarize
    the exact window that triggered the summarization, even if new messages arrive
    before this task runs.
    """
    from src.db.pool import pool

    assert pool is not None, "DB pool not initialised — init_db() must run first"
    async with pool.connection() as conn:
        await maybe_summarize(conn, session_id, message_count, transcript)


@router.post("/sessions", response_model=SessionResponse, status_code=201)
async def create_chat_session(
    body: CreateSessionRequest,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
    token: str = Depends(get_access_token),
):
    """
    Create a new chat session.

    A mock_interview is validated against its preconditions (a job application with a
    description, and a complete profile), bound to that application, and kicked off — the
    interviewer's opening question is generated and persisted so the transcript opens with it.
    """
    if body.session_type == "mock_interview":
        application_id = await _require_interview_ready(token, body.application_id)
        session = await create_session(conn, user_id, body.session_type, application_id)
        await interview.start_interview(conn, session["id"], user_id, application_id, token)
        return session

    return await create_session(conn, user_id, body.session_type)


@router.get("/sessions", response_model=SessionListResponse)
async def list_chat_sessions(
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """List all sessions for the authenticated user."""
    sessions = await get_sessions(conn, user_id)
    return SessionListResponse(sessions=[SessionResponse(**s) for s in sessions])


@router.delete("/sessions/{session_id}", status_code=204)
async def delete_chat_session(
    session_id: str,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """Delete a session and all its messages. Returns 404 if not found or not owned by user."""
    deleted = await delete_session(conn, session_id, user_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Session not found")


@router.get("/sessions/{session_id}/messages", response_model=MessageListResponse)
async def list_session_messages(
    session_id: str,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """Return all messages for a session (oldest first). Verifies ownership."""
    messages = await get_messages(conn, session_id, user_id)
    if messages is None:
        raise HTTPException(status_code=404, detail="Session not found")
    return MessageListResponse(messages=[MessageItem(**m) for m in messages])


@router.post("/sessions/{session_id}/messages", response_model=MessageResponse)
async def send_message(
    session_id: str,
    body: MessageRequest,
    background_tasks: BackgroundTasks,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
    token: str = Depends(get_access_token),
):
    """
    Send a message and get a response. Routes by session type: a mock_interview advances the
    interview (and may end + score it), everything else goes to the career assistant.
    Summarization runs in the background — the user never waits for it.
    """
    meta = await get_session_meta(conn, session_id, user_id)
    if meta is None:
        raise HTTPException(status_code=404, detail="Session not found")

    if meta["session_type"] == "mock_interview":
        if meta["interview_status"] == "completed":
            raise HTTPException(status_code=409, detail="This interview has already ended.")
        outcome = await interview.answer(conn, session_id, user_id, meta["application_id"], body.message, token)
        result = InterviewResult(**outcome["result"]) if outcome["result"] else None
        return MessageResponse(response=outcome["response"], interview=result)

    response = await chat(conn, session_id, user_id, body.message, token)
    # Capture message count and transcript NOW, before any concurrent messages can arrive
    from src.services.chat.utils.history import load_last_n_messages_as_text
    from src.services.chat.utils.summarizer import SUMMARY_EVERY

    total = await count_messages(conn, session_id)
    # Always capture the segment transcript, even if we won't summarize yet
    # (small overhead but ensures correctness)
    transcript = await load_last_n_messages_as_text(conn, session_id, SUMMARY_EVERY)
    background_tasks.add_task(_background_summarize, session_id, total, transcript)
    return MessageResponse(response=response)


@router.post("/sessions/{session_id}/end-interview", response_model=InterviewResult)
async def end_interview(
    session_id: str,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
    token: str = Depends(get_access_token),
):
    """
    End a mock interview early and score it. Ending early is penalised in proportion to how
    many questions went unanswered — the returned score card says so explicitly.
    """
    meta = await get_session_meta(conn, session_id, user_id)
    if meta is None or meta["session_type"] != "mock_interview":
        raise HTTPException(status_code=404, detail="Interview not found")
    if meta["interview_status"] == "completed":
        raise HTTPException(status_code=409, detail="This interview has already ended.")

    outcome = await interview.end_early(conn, session_id, user_id, meta["application_id"], token)
    return InterviewResult(**outcome["result"])
