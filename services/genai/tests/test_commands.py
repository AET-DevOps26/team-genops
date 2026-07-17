"""
Slash command resolution.

The command is detected anywhere in the message, turned into a system-prompt snippet,
and stripped before the text reaches the LLM — so the model never sees "/cover_letter"
as if it were part of what the user wrote.
"""

import pytest


@pytest.mark.parametrize(
    "command,marker",
    [
        ("/cover_letter", "ACTIVE TASK — Cover Letter Generation"),
        ("/resume_tailor", "ACTIVE TASK — Resume Tailoring"),
        ("/fit_analysis", "ACTIVE TASK — Fit Analysis"),
    ],
)
def test_each_command_injects_its_task_context(command: str, marker: str):
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, _ = resolve_command(f"{command} please")

    assert marker in task_context


def test_the_command_token_is_stripped_from_the_message():
    from src.prompts.career_assistant.system.commands import resolve_command

    _, cleaned = resolve_command("/cover_letter for the Zalando role")

    assert cleaned == "for the Zalando role"
    assert "/cover_letter" not in cleaned


def test_a_message_with_no_command_is_untouched():
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, cleaned = resolve_command("what should I write about?")

    assert task_context == ""
    assert cleaned == "what should I write about?"


def test_the_command_is_matched_case_insensitively():
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, cleaned = resolve_command("/COVER_LETTER please")

    assert "Cover Letter Generation" in task_context
    assert cleaned == "please"


def test_the_command_is_found_anywhere_in_the_message():
    """The app prefills text around the command, so it is not always leading."""
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, cleaned = resolve_command("please write /cover_letter for me")

    assert "Cover Letter Generation" in task_context
    assert "/cover_letter" not in cleaned
    assert cleaned.startswith("please write")
    assert cleaned.endswith("for me")


@pytest.mark.parametrize(
    "message",
    [
        "/cover_letter_draft please",  # longer token — must not match the prefix
        "/cover_letters please",
        "/coverletter please",
        "cover_letter please",  # no leading slash
    ],
)
def test_a_lookalike_token_is_not_treated_as_a_command(message: str):
    """
    The trailing \\b stops '/cover_letter' matching inside a longer word — otherwise
    '/cover_letter_draft' would silently trigger cover-letter generation.
    """
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, cleaned = resolve_command(message)

    assert task_context == ""
    assert cleaned == message


def test_the_task_context_is_prefixed_with_a_newline():
    """It is concatenated into the system prompt, so it must not run onto the previous line."""
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, _ = resolve_command("/fit_analysis")

    assert task_context.startswith("\n")


def test_the_first_command_wins_when_several_appear():
    from src.prompts.career_assistant.system.commands import resolve_command

    task_context, _ = resolve_command("/fit_analysis then /cover_letter")

    assert "Fit Analysis" in task_context
    assert "Cover Letter Generation" not in task_context


def test_every_registered_command_resolves():
    """A command added to COMMANDS must be reachable through the regex."""
    from src.prompts.career_assistant.system.commands import COMMANDS, resolve_command

    for command in COMMANDS:
        task_context, _ = resolve_command(f"{command} go")
        assert task_context != "", f"{command} is registered but does not resolve"
