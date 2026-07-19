import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_client import Gauge
from prometheus_fastapi_instrumentator import Instrumentator

from src.db.pool import close_db, init_db
from src.observability import flush as flush_tracing
from src.observability import init_tracing
from src.routers.chat import router as chat_router
from src.routers.job_postings import router as job_postings_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    init_tracing()
    yield
    flush_tracing()
    await close_db()


app = FastAPI(
    title="JobReady GenAI Service",
    version="0.1.0",
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app)

# Constant-1 gauge carrying the running version as a label, so Grafana can
# correlate releases with behaviour changes. APP_VERSION is the image tag
# injected by Helm; "dev" when run locally. Mirrors AppInfoConfig in the
# Spring services.
Gauge("app_info", "Deployed application version (value is constant 1)", ["version"]).labels(version=os.getenv("APP_VERSION", "dev")).set(1)

app.include_router(chat_router)
app.include_router(job_postings_router)


@app.get("/health")
async def health():
    return {"status": "ok"}
