"""
Mock-interview mode.

A mock interview is a self-contained assessment: the interviewer leads, asks a fixed arc of
questions grounded in one job application, and the whole thing is scored once at the end. It
deliberately shares the chat plumbing (sessions, message history, within-session summary) but
NOT cross-session memory — each interview is judged only on what happened inside it, so scores
stay fair and comparable.

Progression is deterministic and owned here, not by the model: the backend counts how many
answers the candidate has given and decides which question comes next (or that it is time to
score). The model only ever writes one question, or the final evaluation.
"""

import json
import logging

from langchain_core.messages import HumanMessage, SystemMessage
from psycopg import AsyncConnection

from src.llm.client import llm
from src.observability import trace_config
from src.prompts.interviewer import EVALUATION_PROMPT, INTERVIEWER_SYSTEM_PROMPT, phase_instruction
from src.services.application_client import get_application
from src.services.chat.session import complete_interview
from src.services.chat.utils.history import (
    count_user_messages,
    load_full_transcript,
    load_history,
    save_message,
)
from src.services.profile_client import get_user_profile

logger = logging.getLogger(__name__)

# The fixed length of the interview arc. Kept in step with the phases defined in the
# interviewer system prompt (opener → technical ×2 → situational → close).
INTERVIEW_QUESTION_COUNT = 5


async def _context(token: str, application_id: str) -> tuple[str, str]:
    """The job (role + JD, grounds the questions) and the candidate profile (personalises them)."""
    job_context = await get_application(token, application_id)
    user_memory = await get_user_profile(token)
    return job_context, user_memory


async def _ask(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    job_context: str,
    user_memory: str,
    question_index: int,
    opening: bool,
) -> str:
    """
    Generate one interviewer turn for the question at `question_index` and persist it.

    For the opener there is no history yet, so a control nudge stands in for the candidate's
    turn (it is never saved — only the interviewer's question is). For later questions the
    saved history already ends with the candidate's latest answer, which the model reacts to.
    """
    system = SystemMessage(
        content=INTERVIEWER_SYSTEM_PROMPT.format(
            job_context=job_context,
            user_memory=user_memory,
            question_number=question_index + 1,
            question_total=INTERVIEW_QUESTION_COUNT,
            phase_instruction=phase_instruction(question_index),
        )
    )

    messages: list = [system]
    if opening:
        # No history yet — a control nudge stands in for the candidate's turn (never saved).
        messages.append(HumanMessage(content="(The interview is starting. Greet me and ask your first question.)"))
    else:
        messages.extend(await load_history(conn, session_id))

    response = await llm.ainvoke(
        messages,
        config=trace_config(user_id=user_id, session_id=session_id, tags=["interview", "question"]),
    )
    question = response.content if isinstance(response.content, str) else str(response.content)
    await save_message(conn, session_id, "assistant", question)
    return question


async def start_interview(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    application_id: str,
    token: str,
) -> str:
    """Kick off a freshly created interview by asking the opening question. Returns it."""
    job_context, user_memory = await _context(token, application_id)
    return await _ask(conn, session_id, user_id, job_context, user_memory, question_index=0, opening=True)


async def answer(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    application_id: str,
    message: str,
    token: str,
) -> dict:
    """
    Record the candidate's answer and either ask the next question or, once the arc is
    complete, score the interview.

    Returns {"response": <text>, "result": <InterviewResult dict | None>}. `result` is set
    only on the turn that ends the interview.
    """
    await save_message(conn, session_id, "user", message)
    answers = await count_user_messages(conn, session_id)

    job_context, user_memory = await _context(token, application_id)

    if answers >= INTERVIEW_QUESTION_COUNT:
        return await _finish(conn, session_id, user_id, job_context, user_memory, answered=answers, ended_early=False)

    # answers == index of the next question (question 0 was the opener, asked before any answer)
    question = await _ask(conn, session_id, user_id, job_context, user_memory, question_index=answers, opening=False)
    return {"response": question, "result": None}


async def end_early(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    application_id: str,
    token: str,
) -> dict:
    """Score an interview the candidate chose to end before the final question."""
    answered = await count_user_messages(conn, session_id)
    job_context, user_memory = await _context(token, application_id)
    return await _finish(conn, session_id, user_id, job_context, user_memory, answered=answered, ended_early=True)


async def _finish(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    job_context: str,
    user_memory: str,
    answered: int,
    ended_early: bool,
) -> dict:
    """Run the evaluation, apply the early-exit penalty, persist, and craft the closing turn."""
    evaluation = await _evaluate(conn, session_id, user_id, job_context, user_memory, answered, ended_early)
    score = evaluation["score"]

    await complete_interview(conn, session_id, score, evaluation)

    if ended_early:
        closing = f"Thanks — we'll stop the interview here. Note that you ended early after {answered} of {INTERVIEW_QUESTION_COUNT} questions, which lowered your score. Your results are ready below."
    else:
        closing = "That's the end of the interview — thank you! Your results and feedback are ready below."
    await save_message(conn, session_id, "assistant", closing)

    return {"response": closing, "result": evaluation}


async def _evaluate(
    conn: AsyncConnection,
    session_id: str,
    user_id: str,
    job_context: str,
    user_memory: str,
    answered: int,
    ended_early: bool,
) -> dict:
    """
    Score the transcript. The model judges the substance of the answers given; the early-exit
    penalty (scaling by coverage) is applied here so it is deterministic and testable, never
    left to the model.
    """
    transcript = await load_full_transcript(conn, session_id)
    early_note = "The candidate ended the interview early, before answering all questions. Judge only the answers given." if ended_early else "The candidate completed all planned questions."
    prompt = EVALUATION_PROMPT.format(
        job_context=job_context,
        user_memory=user_memory,
        questions_answered=answered,
        questions_total=INTERVIEW_QUESTION_COUNT,
        early_note=early_note,
        transcript=transcript,
    )

    response = await llm.ainvoke(
        [SystemMessage(content=prompt)],
        config=trace_config(user_id=user_id, session_id=session_id, tags=["interview", "evaluation"]),
    )
    raw = response.content if isinstance(response.content, str) else str(response.content)
    evaluation = _parse_evaluation(raw)

    base = evaluation.get("overall_score", 0)
    # Early exit is penalised by scaling the substance score to how much of the interview was
    # actually completed — answering 2 of 5 well caps the result at 40% of that quality.
    final = round(base * answered / INTERVIEW_QUESTION_COUNT) if ended_early else base
    final = max(0, min(100, int(final)))

    evaluation["score"] = final
    evaluation["ended_early"] = ended_early
    evaluation["questions_answered"] = answered
    evaluation["questions_total"] = INTERVIEW_QUESTION_COUNT
    return evaluation


def _parse_evaluation(raw: str) -> dict:
    """
    Parse the evaluator's JSON, tolerating markdown fences. On anything unparseable, fall back
    to a neutral record that carries the raw text — a scoring hiccup must not 500 the request
    after a full interview.
    """
    text = raw.strip()
    if text.startswith("```"):
        # Strip a ```json ... ``` fence if the model added one.
        text = text.split("```", 2)[1] if text.count("```") >= 2 else text.strip("`")
        if text.lstrip().lower().startswith("json"):
            text = text.lstrip()[4:]
    try:
        data = json.loads(text)
        if isinstance(data, dict):
            data.setdefault("overall_score", 0)
            return data
    except (json.JSONDecodeError, ValueError):
        logger.warning("interview evaluation was not valid JSON; storing raw fallback")
    return {
        "overall_score": 0,
        "verdict": "Evaluation unavailable",
        "summary": raw.strip()[:1000],
        "competencies": [],
        "strengths": [],
        "improvements": [],
    }
