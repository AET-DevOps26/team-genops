"""Internal machine-to-machine endpoint: classify one email against a user's applications.

Guarded by the static INTERNAL_SERVICE_TOKEN (see `auth.require_internal_token`), not
user JWTs — the caller (the email service's background poller) acts without a live user
request. Not part of api/openapi.yaml, which is the public contract.

Note: an empty candidate list still goes to the LLM — an unmatched-but-relevant
email is the auto-create path.
"""

from fastapi import APIRouter, Depends

from src.auth import require_internal_token
from src.models.email_analysis import EmailAnalysisRequest, EmailAnalysisResult
from src.services.email_analysis import analyze_email

router = APIRouter(
    prefix="/internal/v1",
    tags=["internal"],
    dependencies=[Depends(require_internal_token)],
)


@router.post("/email-analysis", response_model=EmailAnalysisResult)
async def email_analysis(request: EmailAnalysisRequest) -> EmailAnalysisResult:
    return await analyze_email(request)
