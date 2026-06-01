# /document — Update documentation

Update API documentation, README, or architecture notes to reflect current implementation.

## Usage

```
/document [target]
```

Optional target: `api`, `readme`, `architecture`, or a specific file path. No argument → infer from current changes.

## OpenAPI spec (`api/openapi.yaml`)

When any REST endpoint is added, changed, or removed:

1. Update `api/openapi.yaml` — this is the single source of truth.
2. Re-run codegen to regenerate client/server stubs:
   ```sh
   make -C api generate
   ```
3. Verify the generated code compiles (`./mvnw verify` for Spring Boot, `tsc --noEmit` for the web client).
4. Stage both `api/openapi.yaml` and the generated files.

**OpenAPI requirements:**
- Every endpoint must have an `operationId`, a summary, and defined `responses` (at minimum 200, 400, 401).
- Request/response schemas must be defined in `components/schemas` — no inline anonymous objects.
- Security schemes must be declared where JWT is required.

## README (`README.md`)

Update when:
- Setup steps change (new env var, new service, new command)
- A new service is added or a port changes
- Architecture or responsibility of a component changes

Keep the README runnable — someone cloning the repo for the first time must be able to follow it without reverse-engineering anything.

## Architecture docs (`docs/architecture/`)

Update `docs/architecture/microservice_design.md` when:
- A service's bounded context or responsibility changes
- A new communication pattern is introduced
- A scope decision (in v1 / deferred) changes

Diagrams (`.drawio` / `.png`) should be updated when service structure changes, but this may require the user to edit them manually in draw.io.

## Spring Boot OpenAPI annotations

When adding a new endpoint, ensure:
- `@Operation(summary = "...")` on the method
- `@ApiResponse` annotations for each status code
- `@Tag` on the controller class
- `@Schema` on request/response DTOs

## Rules

- Never write documentation that contradicts the code — if in doubt, read the code first.
- Do not add speculative or future-state documentation; document what exists now.
- Keep descriptions concise — one clear sentence is better than a paragraph.
