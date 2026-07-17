"""
SSRF-guarded fetching of a job-posting page, reduced to plain text for the LLM.

This service runs inside the cluster, so a user-supplied URL must never be able
to reach internal services (http://application:8080/...), cloud metadata
endpoints (169.254.169.254), or anything else non-public. Every hop — including
redirects — is validated against the resolved IP addresses before the request
is sent. DNS rebinding between our check and httpx's own resolution is accepted
as out of scope for this threat model.
"""

import asyncio
import ipaddress
from urllib.parse import urlparse

import httpx
from bs4 import BeautifulSoup

_ALLOWED_SCHEMES = {"http", "https"}
_MAX_REDIRECTS = 3
_TIMEOUT_SECONDS = 10.0
_MAX_BYTES = 2 * 1024 * 1024  # 2 MB
_MAX_TEXT_CHARS = 15_000  # keep the LLM prompt bounded
_USER_AGENT = "JobReadyBot/1.0 (+job application assistant)"

# Page furniture that never contains the posting itself.
_STRIP_TAGS = ("script", "style", "noscript", "svg", "nav", "footer", "header", "form")


class JobPostingError(Exception):
    """Fetch/extraction failure carrying the API error contract (code, message, status)."""

    def __init__(self, code: str, message: str, status: int):
        super().__init__(message)
        self.code = code
        self.message = message
        self.status = status


def _invalid(message: str) -> JobPostingError:
    return JobPostingError("URL_INVALID", message, 400)


def _fetch_failed(message: str) -> JobPostingError:
    return JobPostingError("FETCH_FAILED", message, 422)


async def _assert_public_url(url: httpx.URL) -> None:
    """Reject URLs whose scheme is not http(s) or whose host resolves to a non-public IP."""
    if url.scheme not in _ALLOWED_SCHEMES:
        raise _invalid(f"Only http(s) URLs are supported, got scheme '{url.scheme}'.")
    host = url.host
    if not host:
        raise _invalid("The URL has no host.")

    try:
        infos = await asyncio.get_running_loop().getaddrinfo(host, url.port or 0)
    except OSError:
        raise _fetch_failed(f"Could not resolve host '{host}'.") from None

    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_multicast or ip.is_reserved or ip.is_unspecified:
            raise _invalid("The URL points to a private or internal address.")


async def fetch_page_text(url: str) -> str:
    """Fetch a public job-posting page and return its visible text, size-capped."""
    parsed = urlparse(url)
    if not parsed.scheme or not parsed.netloc:
        raise _invalid("The URL is malformed — expected e.g. https://example.com/jobs/123.")

    async def _guard_hop(request: httpx.Request) -> None:
        # Runs for the initial request and for every redirect httpx follows.
        await _assert_public_url(request.url)

    try:
        async with (
            httpx.AsyncClient(
                follow_redirects=True,
                max_redirects=_MAX_REDIRECTS,
                timeout=_TIMEOUT_SECONDS,
                headers={"User-Agent": _USER_AGENT},
                event_hooks={"request": [_guard_hop]},
            ) as client,
            client.stream("GET", url) as response,
        ):
            if response.status_code >= 400:
                raise _fetch_failed(f"The page responded with HTTP {response.status_code}.")
            content_type = response.headers.get("content-type", "")
            if not any(t in content_type for t in ("text/html", "text/plain", "application/xhtml")):
                raise _fetch_failed(f"The page is not HTML (content-type: {content_type or 'unknown'}).")

            chunks: list[bytes] = []
            received = 0
            async for chunk in response.aiter_bytes():
                received += len(chunk)
                if received > _MAX_BYTES:
                    raise _fetch_failed("The page is too large to process (over 2 MB).")
                chunks.append(chunk)
            html = b"".join(chunks).decode(response.encoding or "utf-8", errors="replace")
    except JobPostingError:
        raise
    except httpx.TooManyRedirects:
        raise _fetch_failed("The page redirected too many times.") from None
    except httpx.TimeoutException:
        raise _fetch_failed("The page took too long to respond.") from None
    except httpx.HTTPError as exc:
        raise _fetch_failed(f"The page could not be fetched: {exc.__class__.__name__}.") from None

    return _to_text(html)


def _to_text(html: str) -> str:
    """Strip page furniture and collapse the document to plain text for the prompt."""
    soup = BeautifulSoup(html, "html.parser")
    for tag in soup(_STRIP_TAGS):
        tag.decompose()
    text = " ".join(soup.get_text(" ", strip=True).split())
    if not text:
        raise _fetch_failed("The page contains no readable text.")
    return text[:_MAX_TEXT_CHARS]
