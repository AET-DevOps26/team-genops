from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from psycopg import AsyncConnection

from src.auth import get_current_user_id
from src.db.pool import get_conn
from src.models.schemas import (
    CreateSessionRequest,
    MessageItem,
    MessageListResponse,
    MessageRequest,
    MessageResponse,
    SessionListResponse,
    SessionResponse,
)
from src.services.chat import chat, create_session, delete_session, get_messages, get_sessions
from src.services.chat.utils.history import count_messages
from src.services.chat.utils.summarizer import maybe_summarize

router = APIRouter(prefix="/api/v1/chat", tags=["Chat"])


async def _background_summarize(session_id: str, message_count: int) -> None:
    """
    Runs summarization in the background using its own DB connection.
    message_count is captured at trigger time to ensure we summarize the exact window
    that triggered the summarization, even if new messages arrive before this task runs.
    """
    from src.db.pool import pool
    async with pool.connection() as conn:
        await maybe_summarize(conn, session_id, message_count)


@router.post("/sessions", response_model=SessionResponse, status_code=201)
async def create_chat_session(
    body: CreateSessionRequest,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """Create a new chat session."""
    return await create_session(conn, user_id, body.session_type)


@router.get("/sessions", response_model=SessionListResponse)
async def list_chat_sessions(
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """List all sessions for the authenticated user."""
    sessions = await get_sessions(conn, user_id)
    return SessionListResponse(sessions=sessions)


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
    return MessageListResponse(messages=[MessageItem(**m) for m in messages])


@router.post("/sessions/{session_id}/messages", response_model=MessageResponse)
async def send_message(
    session_id: str,
    body: MessageRequest,
    background_tasks: BackgroundTasks,
    conn: AsyncConnection = Depends(get_conn),
    user_id: str = Depends(get_current_user_id),
):
    """
    Send a message to the career assistant and get a response.
    Summarization runs in the background — the user never waits for it.
    """
    response = await chat(conn, session_id, user_id, body.message)
    # Capture message count now, before any concurrent messages can arrive
    total = await count_messages(conn, session_id)
    background_tasks.add_task(_background_summarize, session_id, total)
    return MessageResponse(response=response)
