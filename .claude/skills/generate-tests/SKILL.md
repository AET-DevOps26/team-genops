# /generate-tests — Generate tests

Generate unit or integration tests for a given file, class, or module.

## Usage

```
/generate-tests <file-or-class>
```

The argument should be a file path or a class/module name. Read the target file before generating tests.

## Steps

1. Read the target file to understand the logic, inputs, outputs, and dependencies.
2. Identify the layer (Spring Boot, Python, or React/TypeScript) from the file path.
3. Generate tests following the layer-specific conventions below.
4. Write the test file to the correct location.
5. Run the tests to confirm they pass (`/test <service>`).

## Spring Boot (JUnit 5 + Mockito)

- Test file location: `services/<service>/src/test/java/<package>/<ClassName>Test.java`
- Use `@ExtendWith(MockitoExtension.class)` for unit tests.
- Use `@SpringBootTest` + `@AutoConfigureMockMvc` only for integration tests that need the full context.
- Mock external dependencies (other services, DB repositories) with `@Mock` / `@MockBean`.
- Never accept `user_id` from anywhere except the JWT claim — test that the controller rejects requests where `user_id` is passed in the body.
- Cover: happy path, invalid input (400), unauthenticated (401), not found (404), and any service-specific edge cases.

## GenAI service (pytest)

- Test file location: `services/genai/tests/test_<module>.py`
- Use `pytest` with `pytest-asyncio` for async FastAPI routes.
- Mock LLM calls with `unittest.mock.patch` or `pytest-mock` — tests must not make real API calls.
- Mock calls to other services (document-service, application-service) at the HTTP client level.
- Cover: happy path, missing/invalid request fields, LLM error handling.

## web-client (Vitest + React Testing Library)

- Test file location: `web-client/src/__tests__/<ComponentName>.test.tsx`
- Use `@testing-library/react` for component tests.
- Mock API calls with `vi.mock` or MSW.
- Cover: renders correctly, user interactions (click, type), loading and error states.

## General rules

- Tests must assert behaviour, not implementation details.
- Do not test private methods directly — test through the public interface.
- Each test case should have a clear name describing what it verifies.
- No skipped or empty tests.
