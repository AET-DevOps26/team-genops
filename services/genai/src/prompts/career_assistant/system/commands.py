"""
Slash command context snippets injected into {task_context} in the system prompt.
Detected via regex anywhere in the user message, then stripped before sending to the LLM.
"""

import re

COMMANDS: dict[str, str] = {
    "/cover_letter": (
        "ACTIVE TASK — Cover Letter Generation:\n"
        "The user wants a tailored cover letter. "
        "Ask for the job description and target company if not already provided. "
        "Output a complete, professional cover letter ready to copy and send. "
        "If the conversation contains the job application's id (a UUID the app "
        "includes when the chat is started from an application), save the final "
        "letter with the save_generated_document tool (document_type "
        '"cover_letter") and tell the user it is attached to their application.'
    ),
    "/resume_tailor": (
        "ACTIVE TASK — Resume Tailoring:\n"
        "The user wants their resume tailored to a specific role. "
        "Ask for the job description if not already provided. "
        "Suggest specific changes: reorder bullet points, highlight relevant skills, "
        "adjust the summary section to mirror the job requirements. "
        "If you produce a full rewritten resume and the conversation contains the "
        "job application's id (a UUID), save it with the save_generated_document "
        'tool (document_type "resume").'
    ),
    "/fit_analysis": (
        "ACTIVE TASK — Fit Analysis:\n"
        "The user wants to understand how well their background matches a role. "
        "Ask for the job description if not already provided. "
        "Provide a structured analysis: strengths (where the profile aligns), "
        "gaps (missing skills or experience), and a recommendation on whether to apply."
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
