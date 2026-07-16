"""Read endpoint for stored emails."""

from __future__ import annotations

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from ..auth import get_current_user_id
from ..db import get_db, list_processed_emails

router = APIRouter(prefix="/api/v1/email", tags=["Email"])


@router.get("/messages")
def list_messages(
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
    user_id: str = Depends(get_current_user_id),
    db: Session = Depends(get_db),
) -> dict:
    """Return the authenticated user's stored emails, newest first."""
    items = list_processed_emails(db, user_id, limit=limit, offset=offset)
    return {"items": items, "limit": limit, "offset": offset}
