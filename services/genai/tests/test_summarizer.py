"""
Rolling session summarization.

The cadence (SUMMARY_EVERY) and the verbatim replay window (HISTORY_WINDOW) are separate
constants on purpose — they were one constant once, which coupled "how much the LLM
replays" to "how often we compress". These tests pin that separation down, and pin the
boundary arithmetic that decides when the LLM is called at all.
"""

import pytest
from langchain_core.runnables import RunnableLambda


class _Response:
    def __init__(self, content: str):
        self.content = content


def _fake_llm(monkeypatch: pytest.MonkeyPatch, content: str) -> list:
    """Replace the module-level llm with a runnable, recording each invocation."""
    from src.services.chat.utils import summarizer

    calls: list = []

    def respond(prompt_value):
        calls.append(prompt_value)
        return _Response(content)

    monkeypatch.setattr(summarizer, "llm", RunnableLambda(respond))
    monkeypatch.setattr(summarizer, "embed_text", _fake_embed)
    return calls


async def _fake_embed(text: str) -> list[float]:
    return [0.1, 0.2, 0.3]


def test_summary_cadence_is_independent_of_the_replay_window():
    """
    Regression guard: these were once the same constant. Compression cadence and how much
    context the LLM replays verbatim are separate decisions, so the summarizer must not
    reach for HISTORY_WINDOW.
    """
    from src.services.chat.utils.history import HISTORY_WINDOW
    from src.services.chat.utils.summarizer import SUMMARY_EVERY

    assert SUMMARY_EVERY == 10
    assert HISTORY_WINDOW == 15
    assert SUMMARY_EVERY != HISTORY_WINDOW


@pytest.mark.asyncio
@pytest.mark.parametrize("message_count", [1, 5, 9, 11, 19, 21])
async def test_no_llm_call_away_from_a_boundary(monkeypatch: pytest.MonkeyPatch, message_count: int):
    """Summarization is expensive; it must fire only on the boundary."""
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import maybe_summarize

    calls = _fake_llm(monkeypatch, "a summary")
    conn = make_conn()

    await maybe_summarize(conn, "session-1", message_count, "User: hi")

    assert calls == []
    assert conn.calls == [], "nothing should be read or written away from a boundary"


@pytest.mark.asyncio
@pytest.mark.parametrize("message_count", [10, 20, 100])
async def test_summarizes_on_each_boundary(monkeypatch: pytest.MonkeyPatch, message_count: int):
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import maybe_summarize

    calls = _fake_llm(monkeypatch, "they discussed Java roles")
    conn = make_conn(results=[[("",)]])

    await maybe_summarize(conn, "session-1", message_count, "User: hi\nAssistant: hello")

    assert len(calls) == 1


@pytest.mark.asyncio
async def test_a_worthless_segment_is_not_written(monkeypatch: pytest.MonkeyPatch):
    """The LLM returning NO_SUMMARY means the segment carried nothing worth keeping."""
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import NO_SUMMARY, maybe_summarize

    _fake_llm(monkeypatch, NO_SUMMARY)
    conn = make_conn()

    await maybe_summarize(conn, "session-1", 10, "User: hi")

    assert conn.calls == [], "NO_SUMMARY must not touch the database"


@pytest.mark.asyncio
async def test_the_new_segment_is_appended_to_the_existing_summary(monkeypatch: pytest.MonkeyPatch):
    """Summaries accumulate — a new segment must not replace the session's history."""
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import maybe_summarize

    _fake_llm(monkeypatch, "second segment")
    conn = make_conn(results=[[("first segment",)]])

    await maybe_summarize(conn, "session-1", 10, "User: hi")

    update_sql, params = conn.calls[-1]
    assert "UPDATE genai.chat_sessions" in update_sql
    assert params[0] == "first segment\n\nsecond segment"
    assert params[-1] == "session-1"


@pytest.mark.asyncio
async def test_the_first_segment_has_no_leading_blank_lines(monkeypatch: pytest.MonkeyPatch):
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import maybe_summarize

    _fake_llm(monkeypatch, "first segment")
    conn = make_conn(results=[[(None,)]])

    await maybe_summarize(conn, "session-1", 10, "User: hi")

    assert conn.calls[-1][1][0] == "first segment"


@pytest.mark.asyncio
async def test_the_pre_captured_transcript_is_what_gets_summarized(monkeypatch: pytest.MonkeyPatch):
    """
    The transcript is captured before the background task runs, so concurrent messages
    cannot change what this summary covers. It must be used rather than re-queried.
    """
    from tests.conftest import make_conn

    from src.services.chat.utils.summarizer import maybe_summarize

    calls = _fake_llm(monkeypatch, "a summary")
    conn = make_conn(results=[[("",)]])

    await maybe_summarize(conn, "session-1", 10, "User: the exact transcript")

    rendered = str(calls[0])
    assert "the exact transcript" in rendered
    # The only SELECT is for the existing summary — never for the transcript.
    selects = [sql for sql, _ in conn.calls if "SELECT" in sql and "chat_messages" in sql]
    assert selects == []
