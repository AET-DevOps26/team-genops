"""
Mock-interview logic.

The valuable, deterministic parts are owned by the backend, not the model: which question
comes next, when the interview ends, how an early exit is penalised, and that a garbled
evaluation never 500s a finished interview. Those are what these tests pin down — the model's
prose is mocked.
"""

from types import SimpleNamespace

import pytest

from tests.conftest import make_conn


class FakeLLM:
    """Records invocations and returns a fixed `.content`, standing in for ChatOpenAI."""

    def __init__(self, content: str):
        self._content = content
        self.calls: list = []

    async def ainvoke(self, messages, config=None):
        self.calls.append(messages)
        return SimpleNamespace(content=self._content)


@pytest.fixture
def _stub_context(monkeypatch: pytest.MonkeyPatch):
    """Stub the outbound job + profile fetches so tests never touch the network."""
    from src.services.chat import interview

    async def fake_application(token, application_id):
        return "Role: Backend Engineer\nJob description: build APIs"

    async def fake_profile(token):
        return "User Profile:\nName: Ada Lovelace"

    monkeypatch.setattr(interview, "get_application", fake_application)
    monkeypatch.setattr(interview, "get_user_profile", fake_profile)


# ---------------------------------------------------------------- the question arc


def test_phase_instruction_walks_the_arc_and_clamps():
    from src.prompts.interviewer import phase_instruction

    assert "Opening" in phase_instruction(0)
    assert "Technical" in phase_instruction(1)
    assert "Closing" in phase_instruction(4)
    # Out-of-range indices clamp to the last phase rather than raising.
    assert phase_instruction(99) == phase_instruction(4)
    assert phase_instruction(-3) == phase_instruction(0)


@pytest.mark.asyncio
async def test_start_interview_asks_the_opener_and_persists_it(monkeypatch, _stub_context):
    from src.services.chat import interview

    monkeypatch.setattr(interview, "llm", FakeLLM("Welcome! Tell me about yourself."))
    # execute order: save_message (assistant opener)
    conn = make_conn(results=[[]])

    question = await interview.start_interview(conn, "s-1", "u-1", "app-1", "tok")

    assert question == "Welcome! Tell me about yourself."
    insert_sql, insert_params = conn.calls[0]
    assert "INSERT INTO genai.chat_messages" in insert_sql
    assert insert_params == ("s-1", "assistant", "Welcome! Tell me about yourself.")


@pytest.mark.asyncio
async def test_answer_below_target_asks_the_next_question(monkeypatch, _stub_context):
    from src.services.chat import interview

    monkeypatch.setattr(interview, "llm", FakeLLM("Good. Now, how would you design a rate limiter?"))
    # execute order: save user answer, count_user_messages -> 2, load_history, save assistant question
    conn = make_conn(results=[[], [(2,)], [("assistant", "Q1"), ("user", "A1")], []])

    outcome = await interview.answer(conn, "s-1", "u-1", "app-1", "my answer", "tok")

    assert outcome["result"] is None
    assert outcome["response"].startswith("Good.")
    # last execute persists the interviewer's next question
    assert conn.calls[-1][1] == ("s-1", "assistant", "Good. Now, how would you design a rate limiter?")


@pytest.mark.asyncio
async def test_answer_at_target_scores_and_completes(monkeypatch, _stub_context):
    from src.services.chat import interview

    evaluation_json = '{"overall_score": 82, "verdict": "Strong", "summary": "Solid answers.", "competencies": [], "strengths": ["clear"], "improvements": ["depth"]}'
    monkeypatch.setattr(interview, "llm", FakeLLM(evaluation_json))
    # save user answer, count -> 5 (== target), load_full_transcript, UPDATE complete, save closing
    conn = make_conn(results=[[], [(5,)], [("user", "A5")], [], []])

    outcome = await interview.answer(conn, "s-1", "u-1", "app-1", "final answer", "tok")

    result = outcome["result"]
    assert result is not None
    assert result["score"] == 82  # completed in full → no penalty
    assert result["ended_early"] is False
    assert result["questions_answered"] == 5
    # the completing UPDATE was issued with the final score
    update = next(c for c in conn.calls if "UPDATE genai.chat_sessions" in c[0])
    assert update[1][0] == 82


@pytest.mark.asyncio
async def test_end_early_penalises_by_coverage_and_says_so(monkeypatch, _stub_context):
    from src.services.chat import interview

    evaluation_json = '{"overall_score": 80, "verdict": "ok", "summary": "s", "competencies": [], "strengths": [], "improvements": []}'
    monkeypatch.setattr(interview, "llm", FakeLLM(evaluation_json))
    # count_user_messages -> 2 answered, load_full_transcript, UPDATE, save closing
    conn = make_conn(results=[[(2,)], [("user", "A1")], [], []])

    outcome = await interview.end_early(conn, "s-1", "u-1", "app-1", "tok")

    result = outcome["result"]
    assert result["ended_early"] is True
    assert result["questions_answered"] == 2
    # 80 * 2/5 = 32
    assert result["score"] == 32
    assert "ended early" in outcome["response"].lower()


# ---------------------------------------------------------------- evaluation parsing


def test_parse_evaluation_strips_a_json_fence():
    from src.services.chat.interview import _parse_evaluation

    parsed = _parse_evaluation('```json\n{"overall_score": 70, "verdict": "ok"}\n```')
    assert parsed["overall_score"] == 70
    assert parsed["verdict"] == "ok"


def test_parse_evaluation_falls_back_on_garbage():
    from src.services.chat.interview import _parse_evaluation

    parsed = _parse_evaluation("the model rambled instead of returning json")
    assert parsed["overall_score"] == 0
    assert "rambled" in parsed["summary"]


# ---------------------------------------------------------------- gate predicates


def test_profile_is_complete_requires_name_and_substance():
    from src.services.profile_client import profile_is_complete

    assert profile_is_complete({"profile": {"first_name": "Ada"}, "skills": [{"name": "Python"}]}) is True
    # name but nothing substantive
    assert profile_is_complete({"profile": {"first_name": "Ada"}}) is False
    # substance but no name
    assert profile_is_complete({"profile": {}, "work_experiences": [{"role": "SWE"}]}) is False


def test_application_has_job_description():
    from src.services.application_client import application_has_job_description

    assert application_has_job_description({"job_description": "build things"}) is True
    assert application_has_job_description({"job_description": "   "}) is False
    assert application_has_job_description({}) is False
