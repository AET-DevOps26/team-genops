"""
Fixtures for the end-to-end suite.

Everything here talks to a running stack over HTTP through the gateway. There are no doubles:
if a test fails, something in the wiring is genuinely broken.
"""

import os
import uuid
from collections.abc import Iterator

import httpx
import pytest

BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:8081")
TIMEOUT = 15.0


class BrowserSession:
    """
    An HTTP client that handles the session cookies the way a browser does.

    Python's `http.cookiejar` cannot model this stack's cookies over a local HTTP origin: it
    files a dotless host as `localhost.local` (so nothing matches on the way back out), and it
    refuses to return `Secure` cookies over plain HTTP. Browsers do neither — they treat
    localhost as a trustworthy origin and send the cookies, which is why dev works. Rather than
    subclass the policy into submission, this keeps the jar explicit and small.

    It honours `Path` so `jr_refresh` is only ever presented to `/api/v1/auth`, exactly as a
    browser would, and it honours `Max-Age=0` so a logout that clears a cookie really clears it
    here too. The `Secure`/`HttpOnly`/`SameSite` attributes are asserted on the wire in
    test_auth_flow, so nothing about this harness lets those regress unnoticed.
    """

    def __init__(self, base_url: str = BASE_URL):
        self._client = httpx.Client(base_url=base_url, timeout=TIMEOUT, follow_redirects=False)
        # name -> (value, path)
        self._jar: dict[str, tuple[str, str]] = {}

    @property
    def base_url(self) -> str:
        return str(self._client.base_url)

    def cookie(self, name: str) -> str | None:
        entry = self._jar.get(name)
        return entry[0] if entry else None

    def set_cookie(self, name: str, value: str, path: str = "/") -> None:
        self._jar[name] = (value, path)

    def _cookie_header(self, url_path: str) -> str | None:
        sendable = [f"{n}={v}" for n, (v, path) in self._jar.items() if url_path.startswith(path)]
        return "; ".join(sendable) if sendable else None

    def _absorb(self, response: httpx.Response) -> None:
        for line in response.headers.get_list("set-cookie"):
            first, *attrs = line.split(";")
            name, _, value = first.strip().partition("=")
            attr_text = ";".join(attrs)
            path = "/"
            for attr in attrs:
                key, _, val = attr.strip().partition("=")
                if key.lower() == "path":
                    path = val
            # A cookie cleared by the server (logout) must disappear from the jar too.
            if "max-age=0" in attr_text.lower() or not value:
                self._jar.pop(name, None)
            else:
                self._jar[name] = (value, path)

    def request(self, method: str, url: str, **kwargs) -> httpx.Response:
        headers = dict(kwargs.pop("headers", {}) or {})
        cookies = self._cookie_header(url)
        if cookies:
            headers["Cookie"] = cookies
        response = self._client.request(method, url, headers=headers, **kwargs)
        self._absorb(response)
        return response

    def get(self, url: str, **kwargs) -> httpx.Response:
        return self.request("GET", url, **kwargs)

    def post(self, url: str, **kwargs) -> httpx.Response:
        return self.request("POST", url, **kwargs)

    def put(self, url: str, **kwargs) -> httpx.Response:
        return self.request("PUT", url, **kwargs)

    def delete(self, url: str, **kwargs) -> httpx.Response:
        return self.request("DELETE", url, **kwargs)

    def close(self) -> None:
        self._client.close()


def _stack_is_up() -> bool:
    try:
        return httpx.get(f"{BASE_URL}/actuator/health/readiness", timeout=3.0).status_code == 200
    except Exception:
        return False


@pytest.fixture(scope="session", autouse=True)
def require_stack() -> None:
    """
    Skip locally when the stack is down; fail hard when CI says it should be up.

    A developer without `docker compose up` should see "skipped", not a wall of red that looks
    like a regression. CI is the opposite case: if the stack failed to start there, skipping
    would report a green build that tested nothing — so `E2E_REQUIRE_STACK=1` turns the skip
    into a failure.
    """
    if _stack_is_up():
        return
    message = f"stack not reachable at {BASE_URL} — run `docker compose up -d --wait`"
    if os.environ.get("E2E_REQUIRE_STACK") == "1":
        pytest.fail(f"{message} (E2E_REQUIRE_STACK=1, so this is a failure, not a skip)", pytrace=False)
    pytest.skip(message, allow_module_level=True)


@pytest.fixture
def client() -> Iterator[BrowserSession]:
    """An anonymous session."""
    session = BrowserSession()
    yield session
    session.close()


def new_email() -> str:
    """A fresh address per call — the database is never reset between runs."""
    return f"e2e-{uuid.uuid4().hex[:12]}@example.test"


class User:
    """A registered user and the session holding their cookies."""

    def __init__(self, client: BrowserSession, email: str, password: str):
        self.client = client
        self.email = email
        self.password = password


def _register(session: BrowserSession) -> User:
    email, password = new_email(), "Correct-Horse-Battery-9"
    response = session.post("/api/v1/auth/register", json={"email": email, "password": password})
    assert response.status_code in (200, 201), f"register failed: {response.status_code} {response.text}"
    return User(session, email, password)


@pytest.fixture
def user() -> Iterator[User]:
    """
    A registered, logged-in user.

    Registration mints the session, so the returned client already carries the cookies —
    exactly the state a browser is in right after signing up.
    """
    session = BrowserSession()
    yield _register(session)
    session.close()


@pytest.fixture
def second_user() -> Iterator[User]:
    """A second user on an independent jar, for cross-tenant checks."""
    session = BrowserSession()
    yield _register(session)
    session.close()
