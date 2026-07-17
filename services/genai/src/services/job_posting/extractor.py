"""
One-shot LLM extraction of job-posting fields from fetched page text.
Mirrors the summarizer's prompt|llm pattern, with structured output so the
response is a validated model instead of free text.
"""

from typing import cast

from src.llm.client import llm
from src.models.schemas import JobPostingExtraction
from src.observability import trace_config
from src.prompts.job_extraction import JOB_EXTRACTION_PROMPT
from src.services.job_posting.fetcher import JobPostingError


async def extract_job_posting(page_text: str, user_id: str) -> JobPostingExtraction:
    """Run the extraction chain over page text. Raises EXTRACTION_FAILED if nothing was found."""
    chain = JOB_EXTRACTION_PROMPT | llm.with_structured_output(JobPostingExtraction)
    result = cast(
        JobPostingExtraction | None,
        await chain.ainvoke(
            {"page_text": page_text},
            config=trace_config(user_id=user_id, tags=["job_extraction"]),
        ),
    )

    if result is None or (not result.company and not result.job_title and not result.job_description):
        raise JobPostingError(
            "EXTRACTION_FAILED",
            "No job-posting fields could be identified on that page.",
            422,
        )
    return result
