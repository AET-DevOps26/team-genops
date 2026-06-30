# Copilot Code Review Instructions

Review this repository as an OpenAPI-first microservice monorepo. Focus only on
high-confidence problems that could make the pull request unsafe to merge.
Trace affected behavior across API contracts, services, persistence, frontend,
and deployment configuration instead of reviewing each changed file in
isolation.

## Review Process

Perform two passes before posting any comment:

1. In the discovery pass, identify possible correctness, security, data-loss,
   contract-breaking, or deployment failures.
2. In the verification pass, attempt to disprove every candidate finding.

Before reporting a finding:

- Re-read the surrounding implementation and trace the affected call path.
- Check whether another layer, validation rule, test, or existing guard already
  handles the case.
- Confirm that the problem was introduced by, or is directly affected by, this
  pull request.
- Confirm a realistic triggering scenario and concrete impact.
- Check for the same root cause elsewhere in the diff and report it only once.

If the evidence or severity is uncertain, do not comment.

## Project Invariants

- Derive `user_id` only from verified JWT claims, never from request bodies,
  query parameters, or custom headers.
- Never expose authentication tokens to browser JavaScript or commit secrets,
  credentials, or private keys.
- Treat `api/openapi.yaml` as the REST contract source of truth. API changes must
  remain consistent with generated clients and DTOs.
- A service must not directly access another service's persistence.
- New environment variables must be documented in `.env.example` and supplied
  through configuration rather than hardcoded.
- Tests must not call real LLM or other paid external services.

## Comment Threshold

Report only merge-blocking defects involving runtime correctness, security,
authorization, data integrity, API compatibility, or deployment failure.

Do not comment on style, naming, formatting, cleanup, optional refactoring,
micro-optimizations, speculative risks, unrelated existing issues, or missing
tests without a specific critical behavior that is unverified. Do not repeat
failures already explained clearly by CI, and do not review generated files as
if they were handwritten.

Each comment must identify the triggering scenario, explain the impact, cite
the relevant evidence, and give a concise direction for resolving the root
cause.
