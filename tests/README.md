# End-to-end tests

Black-box tests that drive the running stack through the **gateway** — the same entry point a
browser uses. Nothing is mocked: real services, real Postgres, real Redis, real JWTs.

These cover what the per-service suites structurally cannot. A unit test doubles the thing next
to it, so the seams stay unverified: that the gateway's `jr_access` cookie really becomes the
`Authorization: Bearer` header a downstream service accepts, that a refresh token really is
single-use once Redis is involved, and that ownership really holds when two users exist at once.

## Running

Bring the stack up first, from the repo root:

```sh
docker compose up -d --wait     # postgres, redis, auth, application, document, gateway
pytest tests/ -v
```

Point them somewhere else with `E2E_BASE_URL` (defaults to `http://localhost:8081`, the gateway):

```sh
E2E_BASE_URL=https://jobready.example pytest tests/ -v
```

If the stack is not reachable, the suite **skips** rather than fails — a red suite should mean
broken behaviour, not a machine without Docker running.

## Scope

`genai` is deliberately excluded: it calls a live LLM, which costs money per run and does not
return deterministic text. Its logic is covered by unit tests in `services/genai/tests/`.

Each test registers its own user with a unique email, so runs are independent and repeatable
against a database that is never reset between runs.
