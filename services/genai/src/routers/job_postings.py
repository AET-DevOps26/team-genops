import logging

from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse

from src.auth import get_current_user_id
from src.models.schemas import JobPostingExtraction, JobPostingExtractRequest
from src.services.job_posting import JobPostingError, extract_job_posting, fetch_page_text

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/job-postings", tags=["JobPostings"])


def _error_response(code: str, message: str, status: int) -> JSONResponse:
    # The spec's unified Error schema ({code, message}), not FastAPI's {"detail": ...}.
    return JSONResponse(status_code=status, content={"code": code, "message": message})


@router.post("/extract", response_model=JobPostingExtraction)
async def extract(
    body: JobPostingExtractRequest,
    user_id: str = Depends(get_current_user_id),
):
    """
    Fetch a public job-posting URL and extract company / job title / description.
    Best-effort: fields the page does not reveal come back null; callers fall
    back to manual entry on any error.
    """
    try:
        page_text = await fetch_page_text(body.url)
        return await extract_job_posting(page_text, user_id)
    except JobPostingError as e:
        return _error_response(e.code, e.message, e.status)
    except Exception:
        # A broken website or a flaky model must never surface as a 500 —
        # the UI treats any error here as "fill the form in manually".
        logger.exception("job-posting extraction failed unexpectedly")
        return _error_response(
            "EXTRACTION_FAILED",
            "Something went wrong while reading that page — fill the fields in manually.",
            422,
        )
