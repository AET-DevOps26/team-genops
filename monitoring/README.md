# Monitoring

Local, opt-in monitoring stack for JobReady. Each tool is a self-contained
compose fragment under its own folder and is pulled together by
`monitoring/docker-compose.yml` via Docker Compose `include:`.

| Tool | Folder | Purpose | Status |
|---|---|---|---|
| Langfuse | [`langfuse/`](./langfuse) | LLM observability (traces, cost, latency) for `genai` | ✅ |
| Prometheus | [`prometheus/`](./prometheus) | Service metrics (request count, latency, error rate) + alert rules | ✅ |
| Grafana | [`grafana/`](./grafana) | Dashboards (as code) over Prometheus | ✅ |

## Prometheus + Grafana

```sh
docker compose -f docker-compose.yml \
               -f monitoring/docker-compose.yml \
               --profile monitoring up
```

- **Prometheus** → http://localhost:9090 (targets, alert state)
- **Grafana** → http://localhost:3001 (admin / `GRAFANA_ADMIN_PASSWORD`, default `admin`; port 3001 because Langfuse owns 3000)

**What gets scraped:** the four Spring services expose Micrometer metrics at
`/actuator/prometheus` on the **management port 8090** — deliberately a separate
port so metrics/health never share the public app port and the ingress can't
reach them by construction. `genai` exposes `/metrics` on its app port
(prometheus-fastapi-instrumentator). Custom metrics: the auth service emits
`auth_login_attempts_total`, `auth_lockouts_total`, `auth_registrations_total`,
`auth_token_refresh_total` (outcome labels only — identity stays in the
`SECURITY_AUDIT` log lines) and every service exposes `app_info{version}` for
release correlation.

**Dashboards are code:** the JSON lives once in
`infra/helm/monitoring/files/dashboards/` — the prod chart renders it into a
ConfigMap and the local compose Grafana mounts the same directory read-only.
Edit the JSON in git, never the UI. `jobready-overview` covers the
course-required RED metrics; `auth-security` covers the custom counters.

**Alert rules:** `infra/helm/monitoring/files/rules.yml` — `ServiceDown`,
`HighErrorRate`, `SlowResponses`, `AuthLockoutSpike`. Same single-source
pattern: the compose Prometheus mounts that file directly, and the prod
`PrometheusRule` is generated from it by the chart — there is no second copy
to keep in sync.

### Production (TUM Rancher, namespace `genops-monitoring`)

Deployed with the Helm chart at
[`infra/helm/monitoring/`](../infra/helm/monitoring/): our own `Prometheus` CR
(reconciled by the platform's cluster-wide Prometheus Operator),
`ServiceMonitor`s targeting the app namespace, the `PrometheusRule` (generated
from the chart's `files/rules.yml`), and a Grafana Deployment provisioned from
ConfigMaps (dashboards rendered straight from the chart's
`files/dashboards/*.json`; a checksum annotation rolls the pod on any dashboard
change), exposed at https://genops-grafana.stud.k8s.aet.cit.tum.de (TLS via the
production cert-manager issuer, admin password from the `grafana-admin` Secret,
which the pipeline creates from the Ansible vault).

**Deploys with prod releases**: `cd-prod` calls
`.github/workflows/cd-monitoring.yml` (a reusable workflow) right after the app
deploy, so the chart ships from the same release tag as the app and
dashboards/alerts never reference metrics prod doesn't emit yet. It runs
Ansible `playbooks/monitoring.yml` (asserts `vault_grafana_admin_password`,
creates the Secret, `helm upgrade`s the chart — passing the Secret's
resourceVersion so a rotated password rolls the Grafana pod). For urgent
dashboard/alert fixes between releases, dispatch `cd-monitoring` manually
(deploys from `main`). Manual equivalent:

```sh
cd infra/ansible
ansible-playbook -i inventories/prod/hosts.yml playbooks/monitoring.yml \
  --vault-password-file <(echo "$VAULT_PASSWORD")
```

Pre-flight check for the operator is documented in the chart's
`templates/prometheus.yaml`; the app chart admits scrapes via
`networkPolicy.monitoringNamespace` (values-prod).

**Documented deviations from `.claude/rules/monitoring_kubernetes.md`:**
1. **Alertmanager is deferred** — rules evaluate and show as firing in
   Prometheus/Grafana, but no delivery channel is wired yet. The
   `alertmanagers` CRD is available when someone picks this up.
2. **Grafana runs stateless (no PV)** — dashboards/datasource are provisioned
   from version-controlled files, so they survive restarts by construction; a PV
   would only let UI edits drift from git, violating "dashboards are code".

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
> holds only the local (compose) side; the shared config (dashboards, alert
> rules) lives in the `infra/helm/monitoring` chart, which both sides consume.
