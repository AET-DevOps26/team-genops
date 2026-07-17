"""
Post-interview evaluation prompt.

Runs once, after the interview ends (either all questions answered, or the candidate ended
early). The model scores what it actually saw; the backend owns the early-exit penalty and
the questions-answered bookkeeping, so those are not the model's to decide — it is told the
coverage as fact and asked to judge the substance.

The model must return a single JSON object and nothing else. The service parses it, so any
prose outside the object breaks scoring.
"""

EVALUATION_PROMPT = """
You are the interviewer, now evaluating the mock interview you just conducted. Score the \
candidate on the substance of their answers — clarity, relevance to the role, technical \
depth, concrete evidence, and communication.

The role interviewed for:
{job_context}

The candidate's profile:
{user_memory}

Coverage: the candidate answered {questions_answered} of {questions_total} planned questions.
{early_note}

Full interview transcript:
{transcript}

Return a SINGLE JSON object and nothing else — no markdown, no commentary before or after. \
Use exactly this shape:
{{
  "overall_score": <integer 0-100, your holistic score for the answers actually given>,
  "verdict": "<one short phrase, e.g. 'Strong hire signal' or 'Needs more preparation'>",
  "summary": "<2-3 sentence overall assessment addressed to the candidate>",
  "competencies": [
    {{"name": "<competency, e.g. Technical depth>", "score": <integer 0-100>, "comment": "<one sentence>"}}
  ],
  "strengths": ["<specific thing they did well>", "..."],
  "improvements": ["<specific, actionable thing to work on>", "..."]
}}

Scoring guidance:
- Judge only what was said. Do not reward answers that were never given.
- Be specific and honest; cite what they actually said. Avoid generic praise.
- Include 3-5 competencies relevant to this role.
- overall_score reflects the quality of the answers given, independent of how many were \
answered — the coverage penalty is applied separately by the system.
"""
