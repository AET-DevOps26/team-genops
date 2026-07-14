"""
Tests for POST /api/v1/job-postings/extract.

Auth is overridden via FastAPI dependency injection; the page fetch and the LLM
chain are monkeypatched so no network or model access happens.
"""

import httpx
import pytest


JOB_HTML = """
<html>
  <head><title>Backend Engineer — Acme</title><style>.x{color:red}</style></head>
  <body>
    <nav>Home | Jobs | About</nav>
    <script>trackPageView()</script>
    <main>
      <h1>Backend Engineer</h1>
      <p>Acme GmbH is hiring. You will build reliable services in Java.</p>
    </main>
    <footer>© Acme</footer>
  </body>
</html>
"""


@pytest.fixture()
def client(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("OPENROUTER_API_KEY", "test")
    from src.auth import get_current_user_id
    from src.main import app

    app.dependency_overrides[get_current_user_id] = lambda: "11111111-1111-1111-1111-111111111111"
    transport = httpx.ASGITransport(app=app)
    yield httpx.AsyncClient(transport=transport, base_url="http://test")
    app.dependency_overrides.clear()


def _mock_http(monkeypatch: pytest.MonkeyPatch, handler) -> None:
    """
    Route the fetcher's httpx.AsyncClient traffic through a MockTransport and
    disable the SSRF guard's DNS resolution (tests must not touch the network).
    The guard itself is covered by the IP-literal rejection tests below, which
    resolve locally without DNS.
    """
    import src.services.job_posting.fetcher as fetcher

    real_client = httpx.AsyncClient

    def factory(**kwargs):
        kwargs["transport"] = httpx.MockTransport(handler)
        return real_client(**kwargs)

    monkeypatch.setattr(fetcher.httpx, "AsyncClient", factory)

    async def allow_all(url):
        return None

    monkeypatch.setattr(fetcher, "_assert_public_url", allow_all)


def _mock_extraction(monkeypatch: pytest.MonkeyPatch, result) -> None:
    import src.routers.job_postings as router_module

    async def fake_extract(page_text: str, user_id: str):
        assert "Backend Engineer" in page_text  # furniture stripped, content kept
        assert "trackPageView" not in page_text
        return result

    monkeypatch.setattr(router_module, "extract_job_posting", fake_extract)


@pytest.mark.asyncio
async def test_happy_path_returns_extracted_fields(client, monkeypatch):
    from src.models.schemas import JobPostingExtraction

    _mock_http(monkeypatch, lambda req: httpx.Response(200, headers={"content-type": "text/html"}, text=JOB_HTML))
    _mock_extraction(monkeypatch, JobPostingExtraction(
        company="Acme GmbH", job_title="Backend Engineer", job_description="Build reliable services in Java."))

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/123"})

    assert response.status_code == 200
    body = response.json()
    assert body["company"] == "Acme GmbH"
    assert body["job_title"] == "Backend Engineer"
    assert body["job_description"] == "Build reliable services in Java."


@pytest.mark.asyncio
async def test_loopback_url_is_rejected(client):
    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "http://127.0.0.1:8080/internal"})

    assert response.status_code == 400
    assert response.json()["code"] == "URL_INVALID"


@pytest.mark.asyncio
async def test_metadata_endpoint_is_rejected(client):
    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "http://169.254.169.254/latest/meta-data/"})

    assert response.status_code == 400
    assert response.json()["code"] == "URL_INVALID"


@pytest.mark.asyncio
async def test_bad_scheme_is_rejected(client):
    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "file:///etc/passwd"})

    assert response.status_code == 400
    assert response.json()["code"] == "URL_INVALID"


@pytest.mark.asyncio
async def test_non_html_content_fails_fetch(client, monkeypatch):
    _mock_http(monkeypatch, lambda req: httpx.Response(200, headers={"content-type": "application/pdf"}, content=b"%PDF"))

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/file.pdf"})

    assert response.status_code == 422
    assert response.json()["code"] == "FETCH_FAILED"


@pytest.mark.asyncio
async def test_http_error_page_fails_fetch(client, monkeypatch):
    _mock_http(monkeypatch, lambda req: httpx.Response(403, headers={"content-type": "text/html"}, text="blocked"))

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/123"})

    assert response.status_code == 422
    assert response.json()["code"] == "FETCH_FAILED"


@pytest.mark.asyncio
async def test_oversized_page_fails_fetch(client, monkeypatch):
    big = b"x" * (2 * 1024 * 1024 + 1)
    _mock_http(monkeypatch, lambda req: httpx.Response(200, headers={"content-type": "text/html"}, content=big))

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/huge"})

    assert response.status_code == 422
    assert response.json()["code"] == "FETCH_FAILED"


@pytest.mark.asyncio
async def test_empty_extraction_reports_extraction_failed(client, monkeypatch):
    import src.routers.job_postings as router_module
    from src.services.job_posting.fetcher import JobPostingError

    _mock_http(monkeypatch, lambda req: httpx.Response(200, headers={"content-type": "text/html"}, text=JOB_HTML))

    async def fake_extract(page_text: str, user_id: str):
        raise JobPostingError("EXTRACTION_FAILED", "No job-posting fields could be identified on that page.", 422)

    monkeypatch.setattr(router_module, "extract_job_posting", fake_extract)

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/123"})

    assert response.status_code == 422
    assert response.json()["code"] == "EXTRACTION_FAILED"


@pytest.mark.asyncio
async def test_unexpected_error_never_500s(client, monkeypatch):
    import src.routers.job_postings as router_module

    async def boom(url: str):
        raise RuntimeError("model exploded")

    monkeypatch.setattr(router_module, "fetch_page_text", boom)

    async with client:
        response = await client.post("/api/v1/job-postings/extract", json={"url": "https://jobs.example.com/123"})

    assert response.status_code == 422
    assert response.json()["code"] == "EXTRACTION_FAILED"
