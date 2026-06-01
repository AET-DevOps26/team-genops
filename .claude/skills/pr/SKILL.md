# /pr — Create a pull request

Create a pull request that follows the project PR template and branch conventions.

## Usage

```
/pr [title]
```

Optional title hint. If not provided, derive from the branch name and commit history.

## Steps

1. Run `git status` to confirm there are no uncommitted changes. If there are, ask the user to commit or stash first.
2. Run `git log main..HEAD --oneline` to review commits included in this PR.
3. Run `git diff main...HEAD --stat` to understand the scope of changes.
4. Confirm the current branch follows the naming convention: `PROJ-<issue-id>/<short-description>` (e.g. `PROJ-I12/add-cover-letter-endpoint`). If it does not, warn the user.
5. Push the branch if it has no upstream: `git push -u origin HEAD`
6. Create the PR using `gh pr create` with the fields below.
7. After the PR is created, set the current user as assignee: `gh pr edit <number> --add-assignee @me`

## PR fields

**Title:** Short, imperative, ≤70 characters. Mirrors the main commit or feature.

**Body:** Fill in the project PR template (`.github/pull_request_template.md`):

```
## Summary
<1-2 sentences: what does this PR do?>
Closes #<issue number>

## Motivation & Context
<Why is this change needed?>

## Changes
- <key implementation decision or change>
- <another if needed>

## Steps for Testing
1. <how to verify this works>
2.

## Checklist
- [ ] Self-reviewed the diff
- [ ] No debug statements, commented-out code, or TODOs
- [ ] All CI checks pass (build · test · lint)
- [ ] Tests added or updated
- [ ] New env vars added to .env.example
- [ ] OpenAPI spec updated and codegen re-run (make -C api generate) if any endpoint changed
- [ ] Screenshots attached for any UI changes
```

## Labels

Apply one or more labels that match the nature of the change:

| Label | When to use |
|---|---|
| `feature` | New user-facing functionality |
| `bug` | Fixing a defect |
| `enhancement` | Improving existing behaviour |
| `infra` | CI/CD, Docker, Kubernetes, tooling |
| `documentation` | README, CLAUDE.md, OpenAPI, architecture docs |
| `planning` | Conventions, process, Definition of Done |
| `server` | Spring Boot backend changes |
| `client` | Frontend changes |
| `genai` | Python GenAI service changes |
| `task` | Technical or process task with no direct user impact |

Use `gh pr edit <number> --add-label "<label1>,<label2>"` to apply.

## Rules

- Target branch is always `main` unless the user specifies otherwise.
- At least one teammate must review and approve before merging.
- Do not merge if CI checks are failing.
