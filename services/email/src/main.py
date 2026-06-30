"""FastAPI application entrypoint.

On startup it applies pending SQL migrations against `email_db` and starts the background
poller; on shutdown it stops the poller. Routers are mounted under `/api/v1/email`.
"""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from .db import engine
from .errors import register_error_handlers
from .migrate import run_migrations
from .poller import start_poller, stop_poller
from .routers import connections, emails

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_: FastAPI):
    run_migrations(engine)
    start_poller()
    try:
        yield
    finally:
        stop_poller()


app = FastAPI(title="JobReady Email Service", version="1.0.0", lifespan=lifespan)
register_error_handlers(app)
app.include_router(connections.router)
app.include_router(emails.router)


@app.get("/health", tags=["Health"])
def health() -> dict:
    return {"status": "ok"}
