"""Tests for Gmail message body extraction (MIME walking, HTML fallback, truncation)."""
from __future__ import annotations

import base64

from src.gmail_client import MAX_BODY_CHARS, extract_body


def _b64(text: str) -> str:
    return base64.urlsafe_b64encode(text.encode()).decode()


def test_plain_text_part_preferred_over_html():
    payload = {
        "mimeType": "multipart/alternative",
        "parts": [
            {"mimeType": "text/html", "body": {"data": _b64("<p>HTML version</p>")}},
            {"mimeType": "text/plain", "body": {"data": _b64("Plain version")}},
        ],
    }
    assert extract_body(payload) == "Plain version"


def test_nested_multipart_is_walked():
    payload = {
        "mimeType": "multipart/mixed",
        "parts": [
            {"mimeType": "application/pdf", "body": {}},
            {
                "mimeType": "multipart/alternative",
                "parts": [
                    {"mimeType": "text/plain", "body": {"data": _b64("Nested text")}},
                ],
            },
        ],
    }
    assert extract_body(payload) == "Nested text"


def test_html_only_message_is_stripped():
    payload = {
        "mimeType": "text/html",
        "body": {"data": _b64("<html><style>p{}</style><p>Hello <b>world</b></p></html>")},
    }
    assert extract_body(payload) == "Hello world"


def test_no_text_parts_returns_none():
    payload = {"mimeType": "application/pdf", "body": {}}
    assert extract_body(payload) is None


def test_body_truncated():
    payload = {"mimeType": "text/plain", "body": {"data": _b64("x" * (MAX_BODY_CHARS + 500))}}
    assert len(extract_body(payload)) == MAX_BODY_CHARS
