# Monitoring

Local, opt-in monitoring stack for JobReady. Each tool is a self-contained
compose fragment under its own folder and is pulled together by
`monitoring/docker-compose.yml` via Docker Compose `include:`.

| Tool | Folder | Purpose | Status |
|---|---|---|---|
| Langfuse | [`langfuse/`](./langfuse) | LLM observability (traces, cost, latency) for `genai` | ✅ |
| Prometheus | `prometheus/` | Service metrics (request count, latency, error rate) | planned |
| Grafana | `grafana/` | Dashboards (as code) over Prometheus + Langfuse | planned |

## Langfuse: local self-hosted vs. cloud deployment

The self-hosted Langfuse stack in [`langfuse/`](./langfuse) is **for local
development only**. It runs six containers (ClickHouse, MinIO, Postgres, Redis,
web, worker) — too heavy and **too expensive to run on our Azure free plan**, and
it's stateful infra we don't want to babysit inside a quota-limited cluster
namespace.

So the split is:

- **Local** → self-hosted Langfuse via docker-compose (this folder). Free,
  unlimited, isolated per developer.
- **Cloud deployment (Azure / TUM)** → the managed **[Langfuse Cloud](https://cloud.langfuse.com)**
  version. The deployed `genai` sends traces to Cloud via keys in the Ansible
  vault (`vault_langfuse_*`), so **nothing about Langfuse is deployed to the
  cluster** — no extra pods, no persistent volumes, no resource cost. See
  [`langfuse/README.md`](./langfuse/README.md) for the Cloud wiring.

### What in this folder relates to Cloud?

For the **running stack, this `monitoring/` folder is local-only** — the
docker-compose Langfuse stack is never deployed and is **not** reached by CD.
Everything that makes the *deployed* app talk to Langfuse Cloud lives **outside**
this folder: `services/genai/src/observability.py` (tracing code),
`infra/helm/jobready/` (the `LANGFUSE_*` env wiring), and
`infra/ansible/` (the vault holding the Cloud keys).

The only shared piece is the pricing: `langfuse/models.json` +
`langfuse/seed_models.py` are the price source-of-truth and the tool used to
register those prices — run once **manually** against a project (local or Cloud).
The Cloud project has already been seeded this way. This is a one-time imperative
action, **not** part of CD.

| Item in `monitoring/` | Cloud-related? |
|---|---|
| `langfuse/docker-compose.yml`, `docker-compose.yml` (self-hosted stack) | ❌ local only — never deployed |
| `langfuse/models.json`, `langfuse/seed_models.py` (pricing) | ⚠️ used once (manually) to seed the Cloud project; otherwise inert |
| Deployed app → Cloud tracing wiring | ✅ lives in `genai` code + Helm + Ansible, **not** here |

## Run it

The stack shares the root compose project's network (so app services can reach
it by name) and is gated behind the `monitoring` profile, so a normal
`docker compose up` never starts it:

```sh
docker compose \
  -f docker-compose.yml \
  -f monitoring/docker-compose.yml \
  --profile monitoring up
```

## Adding a tool later

1. Create `monitoring/<tool>/docker-compose.yml` with `profiles: [monitoring]` on
   each service.
2. Add one line to `monitoring/docker-compose.yml`:
   ```yaml
   include:
     - langfuse/docker-compose.yml
     - <tool>/docker-compose.yml
   ```

Nothing else changes — the run command above stays the same. Prometheus can then
scrape `genai`'s `/metrics` (already exposed) by service name, and Grafana can
add both Prometheus and Langfuse as data sources.

> In Kubernetes these tools belong in a dedicated `monitoring` namespace with
> persistent volumes — see `.claude/rules/monitoring_kubernetes.md`. This folder
> holds the local (compose) side plus the versioned config (dashboards, alert
> rules) that the k8s manifests mount.
