from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from src.db.pool import close_db, init_db
from src.observability import flush as flush_tracing
from src.observability import init_tracing
from src.routers.chat import router as chat_router
from src.routers.internal_analysis import router as internal_analysis_router
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

app.include_router(chat_router)
app.include_router(internal_analysis_router)
app.include_router(job_postings_router)


@app.get("/health")
async def health():
    return {"status": "ok"}
