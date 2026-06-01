# /lint — Lint code

Run linters for one service or all services.

## Usage

```
/lint [service]
```

No argument → lint every layer. Optional `service` argument: `auth`, `application`, `email`, `document`, `genai`, `web-client`.

## Steps per layer

### Spring Boot services (auth, application, email, document)

Checkstyle via Maven:
```sh
cd services/<service>
./mvnw checkstyle:check
```

If Spotless is configured:
```sh
./mvnw spotless:check
```

Auto-fix formatting:
```sh
./mvnw spotless:apply
```

### GenAI service (Python)

```sh
cd services/genai
ruff check src/ tests/
```

Auto-fix:
```sh
ruff check --fix src/ tests/
```

Type checking:
```sh
mypy src/
```

### web-client (React / TypeScript)

```sh
cd web-client
npm run lint
```

TypeScript type check:
```sh
npx tsc --noEmit
```

### OpenAPI spec

```sh
make -C api lint
```

## Notes

- Lint failures block CI — fix them before pushing.
- For Python, prefer `ruff` over `flake8`; it is faster and covers both linting and import sorting.
- Never bypass lint with `--no-verify` or by suppressing rules without a documented reason.
