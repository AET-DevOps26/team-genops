"""
Application service client.

Two properties matter most here and both are load-bearing for behaviour the LLM drives:
the id reaching the URL is always a canonical UUID (it arrives from a model tool call,
so it is untrusted input), and no failure path raises — this client runs on every turn
of a bound session, so an outage must degrade the answer rather than fail the turn.
"""

import httpx
import pytest

APPLICATION = {
    "id": "11111111-2222-3333-4444-555555555555",
    "job_title": "Backend Engineer",
    "company": "Zalando",
    "stage": "applied",
    "job_url": "https://jobs.example/123",
    "company_website": "https://zalando.de",
    "notes": "Referred by Sam",
    "job_description": "Build APIs in Java.",
}


# ---------------------------------------------------------------- untrusted id handling


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "malicious_id",
    [
        "../../users/1",
        "not-a-uuid",
        "",
        "11111111-2222-3333-4444-555555555555/../../admin",
        "*",
    ],
)
async def test_a_non_uuid_id_never_reaches_the_url(mock_http, malicious_id: str):
    """
    The id comes from a model tool call. Interpolating it raw would let a value like
    '../../users/1' retarget the request at another endpoint, so a non-UUID must be
    rejected before any request is made — not merely 404 by the remote service.
    """
    from src.services.application_client import _NOT_FOUND, get_application

    seen = mock_http(lambda request: httpx.Response(200, json=APPLICATION))

    result = await get_application("the-jwt", malicious_id)

    assert result == _NOT_FOUND
    assert seen == [], "a non-UUID id must not produce an outbound request at all"


@pytest.mark.asyncio
async def test_the_id_is_rebuilt_from_the_parsed_uuid(mock_http):
    """Only a canonical id reaches the URL, whatever casing or padding arrived."""
    from src.services.application_client import get_application

    seen = mock_http(lambda request: httpx.Response(200, json=APPLICATION))

    await get_application("the-jwt", "11111111-2222-3333-4444-555555555555".upper())

    assert str(seen[0].url).endswith("/api/v1/applications/11111111-2222-3333-4444-555555555555")


# ---------------------------------------------------------------- get_application


@pytest.mark.asyncio
async def test_get_application_forwards_the_jwt(mock_http):
    from src.services.application_client import get_application

    seen = mock_http(lambda request: httpx.Response(200, json=APPLICATION))

    await get_application("the-jwt", APPLICATION["id"])

    assert seen[0].headers.get("Authorization") == "Bearer the-jwt"


@pytest.mark.asyncio
async def test_get_application_renders_every_section(mock_http):
    from src.services.application_client import get_application

    mock_http(lambda request: httpx.Response(200, json=APPLICATION))

    text = await get_application("the-jwt", APPLICATION["id"])

    assert "Role: Backend Engineer" in text
    assert "Company: Zalando" in text
    assert "Current stage: applied" in text
    assert "Posting: https://jobs.example/123" in text
    assert "Company website: https://zalando.de" in text
    assert "User's notes: Referred by Sam" in text
    assert "Job description:\nBuild APIs in Java." in text


@pytest.mark.asyncio
async def test_get_application_404_asks_the_user_instead(mock_http):
    from src.services.application_client import _NOT_FOUND, get_application

    mock_http(lambda request: httpx.Response(404, json={"code": "NOT_FOUND"}))

    assert await get_application("the-jwt", APPLICATION["id"]) == _NOT_FOUND


@pytest.mark.asyncio
@pytest.mark.parametrize("status_code", [401, 403, 500, 503])
async def test_get_application_degrades_rather_than_raising(mock_http, status_code: int):
    from src.services.application_client import _UNAVAILABLE, get_application

    mock_http(lambda request: httpx.Response(status_code, json={"code": "ERR"}))

    assert await get_application("the-jwt", APPLICATION["id"]) == _UNAVAILABLE


@pytest.mark.asyncio
async def test_get_application_survives_a_transport_error(mock_http):
    """A connection failure must not propagate — the turn still has to answer."""
    from src.services.application_client import _UNAVAILABLE, get_application

    def boom(request: httpx.Request):
        raise httpx.ConnectError("connection refused")

    mock_http(boom)

    assert await get_application("the-jwt", APPLICATION["id"]) == _UNAVAILABLE


# ---------------------------------------------------------------- description bounds


@pytest.mark.asyncio
async def test_a_huge_job_description_is_clamped(mock_http):
    """
    The description is free text pasted from a posting and goes into every turn's prompt
    once a session is bound, so it must not be able to grow the prompt without bound.
    """
    from src.services.application_client import MAX_DESCRIPTION_CHARS, get_application

    # "Q" appears in no other field, so counting it measures the description alone.
    payload = {**APPLICATION, "job_description": "Q" * (MAX_DESCRIPTION_CHARS + 5_000)}
    mock_http(lambda request: httpx.Response(200, json=payload))

    text = await get_application("the-jwt", APPLICATION["id"])

    assert "…" in text
    assert text.count("Q") == MAX_DESCRIPTION_CHARS


@pytest.mark.asyncio
async def test_a_description_at_the_limit_is_left_alone(mock_http):
    from src.services.application_client import MAX_DESCRIPTION_CHARS, get_application

    payload = {**APPLICATION, "job_description": "Q" * MAX_DESCRIPTION_CHARS}
    mock_http(lambda request: httpx.Response(200, json=payload))

    text = await get_application("the-jwt", APPLICATION["id"])

    assert "…" not in text
    assert text.count("Q") == MAX_DESCRIPTION_CHARS


def test_a_missing_description_tells_the_model_to_ask():
    """Without a description a tailored document has only role + company to work from."""
    from src.services.application_client import _format_application

    text = _format_application({**APPLICATION, "job_description": None})

    assert "not provided — ask the user to paste it" in text


def test_format_application_omits_absent_optional_fields():
    from src.services.application_client import _format_application

    text = _format_application({"job_title": "Engineer", "company": "Acme"})

    assert "Role: Engineer" in text
    assert "Posting:" not in text
    assert "Company website:" not in text
    assert "User's notes:" not in text


# ---------------------------------------------------------------- list_applications


@pytest.mark.asyncio
async def test_list_applications_summarises_each_one(mock_http):
    from src.services.application_client import list_applications

    mock_http(lambda request: httpx.Response(200, json={"items": [APPLICATION]}))

    text = await list_applications("the-jwt")

    assert "tracking 1 job application(s)" in text
    assert "- Backend Engineer at Zalando — stage: applied" in text
    assert f"[id: {APPLICATION['id']}]" in text


@pytest.mark.asyncio
async def test_list_applications_omits_descriptions(mock_http):
    """The list is the index the model reads to choose one — keeping it short is the point."""
    from src.services.application_client import list_applications

    mock_http(lambda request: httpx.Response(200, json={"items": [APPLICATION]}))

    text = await list_applications("the-jwt")

    assert "Build APIs in Java." not in text


@pytest.mark.asyncio
async def test_list_applications_flags_a_missing_description(mock_http):
    from src.services.application_client import list_applications

    payload = {"items": [{**APPLICATION, "job_description": None}]}
    mock_http(lambda request: httpx.Response(200, json=payload))

    assert "(no job description saved)" in await list_applications("the-jwt")


@pytest.mark.asyncio
async def test_list_applications_when_there_are_none(mock_http):
    from src.services.application_client import list_applications

    mock_http(lambda request: httpx.Response(200, json={"items": []}))

    assert "no job applications tracked yet" in await list_applications("the-jwt")


@pytest.mark.asyncio
async def test_list_applications_degrades_rather_than_raising(mock_http):
    from src.services.application_client import _UNAVAILABLE, list_applications

    mock_http(lambda request: httpx.Response(500))

    assert await list_applications("the-jwt") == _UNAVAILABLE
