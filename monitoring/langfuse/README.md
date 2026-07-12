# Langfuse — local LLM observability

Self-hosted [Langfuse](https://langfuse.com) for the `genai` service. Every LLM
call (chat turns, summarization) is traced with prompts, completions, tool calls,
token usage, cost and latency, attributed by `user_id` and `session_id`.

This is a **single instance** — a fan-in sink that all `genai` replicas report
into. It does **not** scale with the app.

## Quick start

1. In `.env`, uncomment the two keys (recommended local values):
   ```sh
   LANGFUSE_PUBLIC_KEY=pk-lf-local-dev
   LANGFUSE_SECRET_KEY=sk-lf-local-dev
   ```
2. Start the stack with the `monitoring` profile:
   ```sh
   docker compose -f docker-compose.yml \
                  -f monitoring/docker-compose.yml \
                  --profile monitoring up
   ```
3. Open the UI at <http://localhost:3000> and log in with
   `LANGFUSE_INIT_USER_EMAIL` / `LANGFUSE_INIT_USER_PASSWORD` from `.env`
   (defaults: `admin@genops.com` / `langfuse-admin`).

On first boot Langfuse bootstraps an org/project/user and the API keys above via
`LANGFUSE_INIT_*`, so `genai` traces flow immediately — **no manual UI setup**.

Chat with the app, then watch traces appear under the **Tracing** and
**Sessions** views. The **Users** view slices activity by `user_id`.

## Cost tracking

Token counts (input/output/total) are captured automatically per generation and
rolled up per trace, session, and user.

**Cost** needs one extra step: OpenRouter's model names aren't in Langfuse's
built-in price list, so without prices you'd see tokens but $0 cost. Prices live
as code in [`models.json`](./models.json) and are registered via a small
idempotent seeder — run it once after the stack is up:

```sh
python3 monitoring/langfuse/seed_models.py
```

Current prices:
- `openai/gpt-oss-120b` — **$0.03 / 1M input, $0.15 / 1M output**
- `qwen/qwen3-embedding-8b` — **$0.01 / 1M input** (embeddings are input-only)

To change them, edit `models.json` and re-run the seeder.

### Prices are per-project — seed local AND cloud separately

Model prices live inside a Langfuse **project**, and your local self-hosted
instance and your Langfuse **Cloud** project are different projects. The command
above (default host `http://localhost:3000`) seeds **local**. To price the
**Cloud** project used by the deployed app, run the same seeder once pointed at
Cloud with the Cloud keys:

```sh
LANGFUSE_SEED_HOST=https://cloud.langfuse.com \
LANGFUSE_PUBLIC_KEY=pk-lf-... \
LANGFUSE_SECRET_KEY=sk-lf-... \
python3 monitoring/langfuse/seed_models.py
```

The seeder is idempotent (it paginates existing models and skips ones already
registered), so it's safe to re-run. The current Cloud project has **already been
seeded** with both prices above.

> The embedding price is registered but produces **no cost yet** — embedding
> calls aren't traced (see the genai `observability` module). It takes effect
> automatically if embedding tracing is added later.

> Note: with OpenRouter's default routing a request may land on any provider, and
> providers price the model differently. So this static price is a **close
> estimate**, not exact per-request billing. Token counts are always exact.

## Services

| Service | Image | Role | Exposed |
|---|---|---|---|
| `langfuse-web` | `langfuse/langfuse:3` | UI + ingestion API | `3000` |
| `langfuse-worker` | `langfuse/langfuse-worker:3` | async trace processing | — |
| `langfuse-postgres` | `postgres:16` | transactional store | — |
| `langfuse-clickhouse` | `clickhouse/clickhouse-server:24` | trace analytics store | — |
| `langfuse-minio` | `minio/minio` | S3 blob store (events/media) | `9091` (console) |
| `langfuse-redis` | `redis:7-alpine` | ingestion queue | — |

All state persists in named volumes (`langfuse_*`) across restarts.

## Notes

- **Turning it off:** leave `LANGFUSE_PUBLIC_KEY`/`LANGFUSE_SECRET_KEY` blank in
  `.env`. `genai` detects the missing keys and disables tracing (a no-op) — it
  runs fine with no Langfuse instance.
- **Secrets:** the `LANGFUSE_*` infra passwords in `.env.example` are dev-safe
  defaults. Change them (and `LANGFUSE_ENCRYPTION_KEY`, `openssl rand -hex 32`)
  for any shared or persistent instance.
- **Deployed env:** we do **not** self-host this stateful stack in-cluster. The
  deployed `genai` points at Langfuse Cloud (see below).
- **Footprint:** ~3–4 GB RAM across 6 containers (ClickHouse is the heavy one).
  Local machines only, and only when the `monitoring` profile is up.

## Deployed (Azure / TUM) — enabling Cloud tracing

The deployed `genai` reads its Langfuse keys from the `genai-secrets` Kubernetes
Secret (the same one holding `OPENROUTER_API_KEY`), which Ansible builds from the
vault. The Helm wiring is already in place (`genai.langfuseSecret: genai-secrets`
in `values-dev.yaml` / `values-prod.yaml`), and it's **safe by default**: with no
keys in the vault the values are empty, so `genai` disables tracing — deploys
succeed unchanged.

To turn tracing on:

1. Create a project at <https://cloud.langfuse.com> and copy its `pk-lf-…` /
   `sk-lf-…` API keys.
2. Add them to **both** environment vaults (they're separate):
   ```sh
   ansible-vault edit infra/ansible/inventories/dev/group_vars/all/vault.yml
   ansible-vault edit infra/ansible/inventories/prod/group_vars/all/vault.yml
   ```
   Add these keys:
   ```yaml
   vault_langfuse_host: "https://cloud.langfuse.com"   # or the EU/US host you chose
   vault_langfuse_public_key: "pk-lf-..."
   vault_langfuse_secret_key: "sk-lf-..."
   ```
3. Redeploy (or let CD run). `deploy.yml` writes the keys into `genai-secrets` and
   `genai` starts tracing to Cloud.

> Privacy: enabling this sends real users' chat content to Langfuse Cloud (a third
> party). That's the deliberate tradeoff for cloud tracing — leave the vault keys
> unset to keep deployed tracing off.
