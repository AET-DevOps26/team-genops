# /commit — Create a git commit

Create a well-formed commit following project conventions.

## Usage

```
/commit [message]
```

Optional message hint. If not provided, derive the message from the staged diff.

## Steps

1. Run `git status` and `git diff --staged` to understand what is staged.
2. If nothing is staged, run `git diff` to see unstaged changes and ask the user which files to stage.
3. Derive a commit message if none was provided (see format below).
4. Stage the relevant files (prefer named files over `git add .`).
5. Create the commit.

## Commit message format

```
<type>: <short imperative summary (≤72 chars)>

[optional body — why, not what]
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `infra`

**Examples:**
```
feat: add JWT verification to document service
fix: return 401 instead of 500 on expired token
test: add unit tests for CoverLetterService
ci: add lint step to PR workflow
docs: update OpenAPI spec for /applications endpoint
```

## Rules

- Use the imperative mood: "add", not "adds" or "added".
- The summary line must be ≤72 characters.
- Do NOT add `Co-Authored-By` lines or any AI attribution.
- Do not commit: `.env` files, secrets, generated files that belong in `.gitignore`, or files unrelated to the change.
- If the diff touches an API endpoint, confirm `api/openapi.yaml` is also staged.
- If new env vars are introduced, confirm `.env.example` is also staged.
