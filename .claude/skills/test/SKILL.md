# /test — Run tests

Run the test suite for one service or all services.

## Usage

```
/test [service]
```

No argument → run tests for every layer. Optional `service` argument: `auth`, `application`, `email`, `document`, `genai`, `web-client`.

## Steps per layer

### Spring Boot services (auth, application, email, document)

```sh
cd services/<service>
./mvnw test
```

Run a single test class:
```sh
./mvnw test -Dtest=ClassName
```

Run tests + package (as CI does):
```sh
./mvnw verify
```

### GenAI service (Python)

```sh
cd services/genai
pytest
```

Run a single file:
```sh
pytest tests/test_foo.py
```

Run with coverage:
```sh
pytest --cov=src --cov-report=term-missing
```

### web-client (React / Vite)

```sh
cd web-client
npm test
```

## Running all tests (full sweep)

Run each layer in sequence and report failures:

```sh
for svc in auth; do
  echo "=== $svc ===" && cd services/$svc && ./mvnw test -q && cd -
done
cd services/genai && pytest -q && cd -
cd web-client && npm test -- --run && cd -
```

## Notes

- All tests must pass before opening a PR — CI enforces this.
- If tests fail, read the error output carefully before attempting a fix; do not skip or delete failing tests.
- Integration tests that touch the database require a running postgres container (`docker compose up postgres-db -d`).
