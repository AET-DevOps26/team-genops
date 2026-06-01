# /review — Code review

Review the staged diff or a specified file/PR for correctness, security, and project standards.

## Usage

```
/review [file or PR number]
```

No argument → review the current branch diff against `main`. Optional argument: a file path or a GitHub PR number.

## Review checklist

Work through each category and call out any issues found.

### Correctness
- Does the logic do what the PR description claims?
- Are edge cases handled (null inputs, empty lists, missing env vars)?
- Do tests cover the critical paths changed?

### Security
- Is `user_id` **always** extracted from the JWT claim — never from request body, query string, or custom header?
- Are there SQL injection, XSS, or command-injection risks?
- Are secrets or credentials hardcoded anywhere?
- Are new env vars added to `.env.example`?

### API contract
- If any endpoint changed, was `api/openapi.yaml` updated and codegen re-run (`make -C api generate`)?
- Do generated DTOs match the spec — no hand-written request/response classes?

### DevOps / deployment
- If a new service or port was added, is it in `docker-compose.yml`?
- Are new environment variables externalised (not hardcoded)?
- If Kubernetes manifests changed, are secrets managed correctly?

### Testing
- Are new or changed behaviours covered by tests?
- Do tests assert behaviour, not implementation details?
- No skipped or empty test cases?

### Code quality
- No debug statements, commented-out code, or stray TODOs?
- Names are clear and consistent with the rest of the codebase?
- No premature abstractions or features beyond the PR scope?

## Definition of Done

Before approving, confirm all DoD criteria are met:

- [ ] Feature works end-to-end (manually verified or covered by tests)
- [ ] Unit/integration tests written or updated for new logic
- [ ] All CI checks pass (build · test · lint)
- [ ] PR reviewed and approved by at least one teammate
- [ ] `api/openapi.yaml` updated and codegen re-run if any endpoint changed
- [ ] New env vars added to `.env.example`
- [ ] `CLAUDE.md` or `docs/` updated if behaviour, ports, or architecture changed

## Output format

Summarise findings as:
- **Must fix** — blocks merge
- **Should fix** — important but not blocking
- **Nit** — minor style or preference

End with an overall recommendation: Approve / Request changes.
