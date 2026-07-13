"""
Slash command context snippets injected into {task_context} in the system prompt.
Detected via regex anywhere in the user message, then stripped before sending to the LLM.
"""

import re

COMMANDS: dict[str, str] = {
    "/cover_letter": (
        "ACTIVE TASK — Cover Letter Generation:\n"
        "Write a complete, tailored cover letter, ready to copy and send.\n"
        "Ground it in the target job application and the user's profile above: name the "
        "company and role, and connect the user's actual experience, skills and education "
        "to the specific requirements in the job description.\n"
        "Use ONLY facts given above. Never invent an email address, phone number, postal "
        "address, date or employer — a fabricated contact detail is worse than a missing one, "
        "because the user may send the letter without noticing. Omit any letterhead line you "
        "do not have; do not substitute a placeholder like [Your Email] or a plausible-looking "
        "value. Start from the greeting if no contact details are known.\n"
        "Ask for the job description only if none is available above.\n"
        "Once the letter is final and an application id is known, save it with the "
        'save_generated_document tool (document_type "cover_letter") and tell the user it '
        "is attached to their application."
    ),
    "/resume_tailor": (
        "ACTIVE TASK — Resume Tailoring:\n"
        "Tailor the user's resume to the target job application above, using their profile "
        "as the source of truth for experience, education and skills.\n"
        "Mirror the language of the job description, lead with the most relevant experience, "
        "and make each bullet concrete (impact and scope, not duties). Never invent roles, "
        "employers, dates or qualifications the user does not have.\n"
        "Ask for the job description only if none is available above.\n"
        "When you produce a full tailored resume and an application id is known, save it "
        'with the save_generated_document tool (document_type "resume").'
    ),
    "/fit_analysis": (
        "ACTIVE TASK — Fit Analysis:\n"
        "Assess how well the user's background matches the target job application above.\n"
        "Give a structured analysis: strengths (where the profile genuinely aligns, citing "
        "specific experience), gaps (requirements the profile does not evidence), and a clear "
        "recommendation on whether to apply and what to emphasise.\n"
        "Ask for the job description only if none is available above."
    ),
}

# Matches any registered slash command, case-insensitive
# Uses word boundary after the command to prevent partial matches (e.g., /cover_letter_draft)
# but allows the leading / which doesn't work with \b on both sides
_COMMAND_RE = re.compile(
    r"(?i)(" + "|".join(re.escape(cmd) for cmd in COMMANDS) + r")\b"
)


def resolve_command(message: str) -> tuple[str, str]:
    """
    Scan the message for a registered slash command using regex.
    Returns (task_context, cleaned_message):
      - task_context  — injected system prompt snippet, empty string if no match
      - cleaned_message — original message with the command token stripped
    """
    match = _COMMAND_RE.search(message)
    if match:
        command = match.group(1).lower()
        cleaned = _COMMAND_RE.sub("", message).strip()
        return f"\n{COMMANDS[command]}", cleaned
    return "", message
