"""
Unit tests for extract_job_posting (src/services/job_posting/extractor.py).

The LLM chain is replaced with a RunnableLambda so no model is called; these
assert the "nothing extracted → EXTRACTION_FAILED" guard and the happy path.
The endpoint-level flow is covered separately in test_job_extraction.py.
"""

import pytest
from langchain_core.runnables import RunnableLambda

from src.models.schemas import JobPostingExtraction
from src.services.job_posting import extractor
from src.services.job_posting.fetcher import JobPostingError


def _stub_chain(monkeypatch: pytest.MonkeyPatch, result: JobPostingExtraction | None):
    """Make `llm.with_structured_output(...)` yield a runnable returning `result`."""

    async def _return(_inputs):
        return result

    class FakeLLM:
        def with_structured_output(self, _schema):
            return RunnableLambda(_return)

    monkeypatch.setattr(extractor, "llm", FakeLLM())


@pytest.mark.asyncio
async def test_returns_extraction_when_fields_present(monkeypatch: pytest.MonkeyPatch):
    _stub_chain(
        monkeypatch,
        JobPostingExtraction(company="Acme", job_title="Engineer", job_description="Build things"),
    )

    result = await extractor.extract_job_posting("some page text", "user-1")

    assert result.company == "Acme"
    assert result.job_title == "Engineer"


@pytest.mark.asyncio
async def test_raises_when_model_returns_none(monkeypatch: pytest.MonkeyPatch):
    _stub_chain(monkeypatch, None)

    with pytest.raises(JobPostingError) as exc:
        await extractor.extract_job_posting("page", "user-1")

    assert exc.value.code == "EXTRACTION_FAILED"
    assert exc.value.status == 422


@pytest.mark.asyncio
async def test_raises_when_all_fields_empty(monkeypatch: pytest.MonkeyPatch):
    _stub_chain(monkeypatch, JobPostingExtraction(company=None, job_title=None, job_description=None))

    with pytest.raises(JobPostingError) as exc:
        await extractor.extract_job_posting("page", "user-1")

    assert exc.value.code == "EXTRACTION_FAILED"
