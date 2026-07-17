"""
Shared test fixtures.

`src.config.Settings` instantiates at import time and requires `openrouter_api_key`,
so the env has to be set before any `src.*` import — hence module level, not a fixture.
Setting it explicitly (rather than `setdefault`) also detaches the suite from the repo
root `.env` that pydantic-settings would otherwise read on a developer machine.
"""

import os

os.environ["OPENROUTER_API_KEY"] = "test-key"

from typing import Any  # noqa: E402

import httpx  # noqa: E402
import pytest  # noqa: E402


@pytest.fixture
def mock_http(monkeypatch: pytest.MonkeyPatch):
    """
    Route every `httpx.AsyncClient` request made by the module under test to a handler.

    Returns a callable taking the handler, so a test can assert on the outbound request:

        requests = mock_http(lambda req: httpx.Response(200, json={...}))
    """

    def install(handler) -> list[httpx.Request]:
        seen: list[httpx.Request] = []
        real_client = httpx.AsyncClient

        def recording(request: httpx.Request) -> httpx.Response:
            seen.append(request)
            return handler(request)

        def factory(**kwargs):
            kwargs["transport"] = httpx.MockTransport(recording)
            return real_client(**kwargs)

        monkeypatch.setattr(httpx, "AsyncClient", factory)
        return seen

    return install


class FakeCursor:
    """Stands in for a psycopg cursor: yields pre-canned rows."""

    def __init__(self, rows: list[tuple] | None = None, rowcount: int = 0):
        self._rows = rows if rows is not None else []
        self.rowcount = rowcount

    async def fetchone(self) -> tuple | None:
        return self._rows[0] if self._rows else None

    async def fetchall(self) -> list[tuple]:
        return self._rows


class FakeConn:
    """
    Stands in for a psycopg AsyncConnection.

    `results` is consumed in order, one per `execute` — a list of rows becomes that
    cursor's result, and a FakeCursor is used as-is (for asserting on rowcount).
    Every statement is recorded in `.calls` so tests can assert on SQL and params.
    """

    def __init__(self, results: list[Any] | None = None):
        self._results = list(results or [])
        self.calls: list[tuple[str, Any]] = []

    async def execute(self, sql: str, params: Any = None) -> FakeCursor:
        self.calls.append((sql, params))
        if not self._results:
            return FakeCursor()
        nxt = self._results.pop(0)
        return nxt if isinstance(nxt, FakeCursor) else FakeCursor(nxt)

    def sql_at(self, index: int) -> str:
        """The statement at `index`, whitespace-normalised so assertions can ignore layout."""
        return " ".join(self.calls[index][0].split())

    def params_at(self, index: int) -> Any:
        return self.calls[index][1]


def make_conn(results: list[Any] | None = None) -> Any:
    """
    A FakeConn, typed as Any.

    psycopg's AsyncConnection is a large generic protocol and the code under test touches
    a sliver of it; implementing the whole thing in a double would be more code than the
    tests it serves. `Any` lets the double stand in wherever a connection is expected while
    still exposing `.calls` for assertions — the trade is deliberate and scoped to tests.
    """
    return FakeConn(results)
