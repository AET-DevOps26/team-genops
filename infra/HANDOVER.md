# JobReady Deployment — Handover

> Status as of 2026-06-30. Branch `PROJ-I08/set-up-terraform-for-e2e-authentication`.
> Author handover for whoever continues this work. Operational steps live in
> [`infra/README.md`](./README.md); this doc explains **what exists, why, and what's left**.

---

## 1. TL;DR status

| Area | State |
|---|---|
| Helm chart (auth, web-client, postgres, redis, gateway) | ✅ done, `helm lint` clean, renders for both envs |
| Spring Cloud Gateway service | ✅ built, run, routes verified locally |
| CI image build/push (GHCR) | ✅ workflow written, YAML valid |
| Terraform (AKS dev) | ✅ `terraform validate` passes |
| Ansible (bootstrap + deploy) | ✅ playbooks written, YAML valid |
| CD pipelines (dev + prod) | ✅ workflows written, YAML valid |
| **Actually deployed anywhere** | ❌ not yet — needs credentials + 3 external facts (see §6) |

**Nothing has been applied to a cluster.** All artifacts are code. The remaining work is
operational (secrets, confirm cluster facts, run the pipelines) — see §6.

---

## 2. Architecture

### Two environments, one chart

```
                 ┌─────────────────────────── DEV (Azure AKS) ───────────────────────────┐
  git push main  │  Terraform → RG + AKS + reserved static IP (cloudapp.azure.com FQDN)   │
  ───────────►   │  Ansible   → installs ingress-nginx + cert-manager, then deploys chart │
                 └───────────────────────────────────────────────────────────────────────┘
                 ┌──────────────────────── PROD (TUM Rancher `stud`) ───────────────────────┐
  release        │  platform already provides ingress-nginx + cert-manager + public DNS     │
  ───────────►   │  Ansible   → deploys chart into namespace ge86yog-devops-genops          │
                 └──────────────────────────────────────────────────────────────────────────┘
```

### Request flow (both environments are identical from here down)

```
Browser ──HTTPS──► Ingress (TLS terminates here, cert from cert-manager)
                     │  host: dev → *.cloudapp.azure.com   prod → *.stud.k8s.aet.cit.tum.de
                     ▼
                   gateway (Spring Cloud Gateway)
                     ├─ /api/**  ─► auth      ─► postgres + redis
                     └─ /**      ─► web-client (SPA)
```

Single host, single origin → `SameSite=Strict` HttpOnly cookies work without CORS.

### Tool ownership (the rule that keeps this clean)

| Tool | Owns | Stops at |
|---|---|---|
| **Terraform** | Azure cloud only (RG, AKS, ingress IP) | the kubeconfig — no k8s/helm providers |
| **Ansible** | all Kubernetes config + Helm deploys, both envs | — it is the only thing that runs Helm |
| **Helm** | packaging (the chart + 2 values files) | invoked only by Ansible |
| **GitHub Actions** | CI (images) + CD (runs terraform/ansible) | — |

---

## 3. Key decisions and *why*

These were worked out interactively; capturing the reasoning so they aren't re-litigated.

### 3.1 dev = Azure AKS, prod = TUM Rancher
The TUM cluster is the **required** deployment target (course), but you only get a
namespace-scoped tenant account there — no admin, no cluster add-ons, a kubeconfig token
that **expires**. Azure is where you have full control. So: **prod = TUM** (the graded
target, free, platform provides DNS+TLS) and **dev = Azure AKS** (your sandbox, and the
thing that gives Terraform a real cloud job). The inversion (paying for dev, free prod) is
deliberate. Parity is preserved by using the **same Helm chart** on both, differing only in
two values files.

### 3.2 In-cluster API gateway, not an Azure/managed one
Workloads run in the TUM cluster; the only public entry there is the platform Ingress (no
LoadBalancer for tenants). A managed Azure gateway would have to reach back into TUM, which
needs the TUM ingress publicly reachable anyway — extra hops for nothing. So the gateway is
an **in-cluster Deployment**, and Azure (if ever used for DNS) is just a CNAME. Currently we
don't even need Azure DNS (see 3.6).

### 3.3 Gateway = Spring Cloud Gateway
Chosen over Kong/Traefik because the team is all-Spring (auth/application/document are Spring
Boot), so it's the same build/test/deploy pipeline and language — lowest risk. **NGINX Gateway Fabric** (the repo's original
plan) was rejected: it needs the **Gateway API CRDs installed cluster-wide**, which a tenant
token cannot do. The gateway is **routing-only by design** — the **auth service already
handles cookie→Bearer itself** (services read the `jr_access` cookie directly), so the gateway
needs no auth filter. It is purely the single-origin entry point that fans `/api/**` to auth
and `/**` to the web-client.

Pinned versions came from `start.spring.io` (authoritative), not guessed: **Spring Boot
4.0.6 → Spring Cloud 2025.1.2 → `spring-cloud-starter-gateway-server-webmvc` 5.0.2** (the new
servlet gateway). Route config prefix is `spring.cloud.gateway.server.webmvc.routes`.

### 3.4 Images on GHCR, built by GitHub Actions, private + pull secret
One registry for both clusters. The repo lives under `AET-DevOps26`, so Actions can push to
`ghcr.io/aet-devops26/*` with the built-in `GITHUB_TOKEN` — no external creds. Packages are
**private**, so each namespace needs a `ghcr-pull` dockerconfigjson secret (Ansible creates
it from the vault). Tags: `sha-<short>` (for precise CD pins) + `latest` + semver on releases.

### 3.5 Ansible owns Helm (Terraform does not)
Terraform *could* deploy via its helm provider, and Ansible *could* via `kubernetes.core.helm`
— but letting both touch the release causes drift. We picked **one owner: Ansible**. Terraform
stops at "AKS exists + kubeconfig". Note AKS nodes are managed, so Ansible's classic SSH/VM
role doesn't apply — it operates purely at the Kubernetes API level via the `kubernetes.core`
collection.

### 3.6 TLS + DNS come free from the platforms
- **prod (TUM):** the cluster runs cert-manager and a wildcard public DNS
  (`*.stud.k8s.aet.cit.tum.de`). Just set the ingress host + the **production** ClusterIssuer
  annotation. ⚠️ A target we inspected used the **Let's Encrypt _staging_** issuer → untrusted
  cert → the `Secure` auth cookies silently break. Use the production issuer.
- **dev (AKS):** Ansible installs cert-manager + a Let's Encrypt **production** ClusterIssuer
  (HTTP-01). The hostname is a **reserved static IP with an Azure DNS label** →
  `jobready-dev.<region>.cloudapp.azure.com` — stable, free, no domain to buy, no Azure DNS
  zone. The IP lives in the main RG (not the node RG) with `prevent_destroy`, so the URL and
  cert survive a cluster rebuild.

### 3.7 Redis is part of the deploy (was missing)
auth depends on `spring-boot-starter-data-redis` (server-side sessions for the split-token
BFF). It was in `docker-compose` but **absent from the chart** — without it auth's health
reports DOWN and the pod never becomes Ready. Added as a chart component.

### 3.8 Stable RSA JWT keys via Secret
If `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` are blank, auth generates an **ephemeral** keypair at
startup (tokens die on every restart, differ per replica). Since this branch is about
authentication, we inject stable keys from an `auth-jwt` Secret. Format: **PKCS#8** private
(`BEGIN PRIVATE KEY`) + **X.509** public (`BEGIN PUBLIC KEY`), matching `JwtConfig.java`'s
`RsaKeyConverters.pkcs8()/.x509()`.

---

## 4. Repo map

```
infra/
├── HANDOVER.md            ← this file
├── README.md              ← operational runbook (commands)
├── terraform/             ← Azure dev infra
│   ├── versions.tf        ← providers + azurerm remote-state backend
│   ├── providers.tf       ← azurerm provider
│   ├── variables.tf       ← region, node size/count, dns_label, …
│   ├── main.tf            ← RG, AKS, reserved public IP, role assignment
│   └── outputs.tf         ← ingress_ip, ingress_fqdn, kube_config
├── ansible/
│   ├── ansible.cfg, requirements.yml (kubernetes.core)
│   ├── inventories/{dev,prod}/hosts.yml + group_vars/all.yml
│   ├── group_vars/vault.example.yml   ← template; copy+encrypt per env
│   └── playbooks/{bootstrap.yml,deploy.yml}
└── helm/jobready/
    ├── values.yaml         ← defaults + every new knob (documented inline)
    ├── values-dev.yaml     ← AKS overrides
    ├── values-prod.yaml    ← TUM overrides (HAS TODOs — see §6)
    └── templates/{auth,web-client,postgres,redis,gateway}/…, ingress.yaml, namespace.yaml

services/gateway/           ← new Spring Cloud Gateway service (+ Dockerfile, .dockerignore)
.github/workflows/
├── build-images.yml        ← CI: build+push 3 images to GHCR
├── ci-gateway.yml          ← PR build+test for gateway
├── cd-dev.yml              ← CD: terraform apply → ansible (on images published)
└── cd-prod.yml             ← CD: ansible deploy (on release, manual-approval env)
```

---

## 5. What has been verified (and what hasn't)

**Verified locally:**
- `helm lint` clean; chart renders for dev and prod; all containers (incl. the postgres
  init container) carry resource limits; prod skips the Namespace, dev creates it; ingress
  routes to the gateway.
- Gateway **compiles, starts, and serves** — health probes 200, routes load (`/api/**`→auth,
  `/**`→web-client confirmed via the running app).
- `terraform fmt` + `init` + `validate` pass.
- All Ansible / workflow YAML parses.

**NOT verified (needs a real cluster + creds):** any actual `helm install`, `terraform apply`,
`ansible-playbook` run, and the auth probe paths (`/actuator/health/readiness|liveness`) —
confident they're exposed (`management.endpoint.health.probes.enabled=true`) but smoke-test
them; if 404, fall back to `/actuator/health`.

---

## 6. What needs to be done (prioritized)

### Blockers — must do before any deploy
1. **Confirm 3 facts** and fill `infra/helm/jobready/values-prod.yaml`:
   - `ingress.host` (exact name under `*.stud.k8s.aet.cit.tum.de`)
   - `ingress.ingressClassName` (Rancher UI → Ingresses → Create shows it)
   - `ingress.clusterIssuer` (the **production** issuer — not staging)
2. **First prod deploy by hand** (the tracer bullet) using the runbook in `README.md §"Manual
   prod deploy"`. Proves the path before automating.

### To enable CD
3. **GitHub secrets:** `AZURE_CLIENT_ID/TENANT_ID/SUBSCRIPTION_ID`, `TFSTATE_RG/TFSTATE_SA`,
   `RANCHER_KUBECONFIG`, `ANSIBLE_VAULT_PASSWORD`.
4. **GitHub Environments:** create `dev` and `production`; add a **required reviewer** on
   `production` (cd-prod relies on it for the approval gate).
5. **Encrypted vaults:** `cp ansible/group_vars/vault.example.yml
   ansible/inventories/<env>/group_vars/vault.yml && ansible-vault encrypt …` for dev and prod.
   Commit the encrypted files.
6. **Azure prerequisites for Terraform:** a service principal/OIDC federated credential, and
   the state storage account/container referenced in `cd-dev.yml`.

### Follow-up / known debt
7. **Rancher token expiry:** `RANCHER_KUBECONFIG` will stop working when the token TTL lapses
   → refresh it, or use a long-lived API token if the course allows.
8. **DB migrations:** auth uses Hibernate `ddl-auto=update` (intentional for now). Revisit
   Flyway once the schema stabilises.
9. **cd-dev runs `terraform apply` on every image publish** — idempotent (usually a no-op),
   but consider splitting infra apply into its own gated workflow.
10. Add **genai/application/document/email** to the chart + image matrix when they're built
    (out of scope for this slice).

---

## 7. Gotchas / risks to remember

- **Secure cookies ⇒ trusted HTTPS required.** A staging/self-signed cert makes login *appear*
  to work but the browser drops the cookie. Always verify a green padlock.
- **Prod namespace has a ResourceQuota on `limits.*` and no LimitRange** → every container
  (init included) MUST set limits or the whole pod is rejected. The chart does this; keep it
  true for anything you add.
- **cert-manager HTTP-01** needs `/.well-known/acme-challenge/*` to reach the solver, not the
  gateway. ingress-nginx handles the precedence (more-specific path), but if certs don't issue
  on dev, check that first.
- **`prevent_destroy` on the ingress IP** means `terraform destroy` fails by design; remove the
  lifecycle block intentionally if you really want to tear the IP down.
- **`kubectl cluster-info` fails for the TUM tenant** (it reads `kube-system`) — that's normal,
  not a broken cluster.

### Deep-review fixes already applied (2026-06-30)
- **Namespace double-create (dev):** Ansible creates the namespace (secrets need it first), so
  the chart no longer does — `values-dev.yaml` is `namespace.create: false`. Otherwise Helm
  errored on a Namespace it didn't own.
- **Helm missing in CD:** `kubernetes.core.helm` shells out to the `helm` binary; both CD
  workflows now install it (`azure/setup-helm`, pinned v3.16.0).
- **Dev ingress host:** `cd-dev` now exports the Terraform `ingress_fqdn` and passes it
  (`-e ingress_host=`), so the dev ingress/cert use the real `cloudapp.azure.com` name instead
  of the `REGION` placeholder.
- **Release tag mismatch:** image semver tags drop the `v` (`v1.2.3`→`1.2.3`); `cd-prod` now
  strips it before deploying.
- **Init container limits** + **`.terraform/` gitignored** (232 MB provider binary).

### Residual quirks (acceptable for now — know they exist)
- **Helm version skew:** chart validated locally on Helm 4.2 but CD pins **v3.16.0** (safer with
  `kubernetes.core`, and the chart uses only Helm-3-compatible features).
- **Azure RBAC propagation:** the cluster's Network-Contributor role assignment can lag a minute
  behind the first ingress LB reconcile; ingress-nginx retries, so it self-heals.
- **Prod release race:** `build-images` and `cd-prod` both fire on a release; the `production`
  **approval gate** is what serialises them (approve only after images finish). Don't remove it.
- **Gateway `/actuator` is reachable through the ingress** — `/actuator/gateway/routes` lists
  internal service URIs. Minor info leak; tighten `management.endpoints.web.exposure` later.
- **`.terraform.lock.hcl`** holds only `linux_amd64` hashes (what CI uses). macOS teammates run
  `terraform providers lock -platform=darwin_arm64 -platform=linux_amd64`.

---

## 8. How to re-verify locally

```sh
# chart
helm lint infra/helm/jobready
helm template jobready infra/helm/jobready -n ge86yog-devops-genops -f infra/helm/jobready/values-prod.yaml

# gateway
cd services/gateway && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -q -DskipTests package
java -jar target/gateway-*.jar &   # then: curl localhost:8081/actuator/health/readiness

# terraform
cd infra/terraform && terraform init -backend=false && terraform validate
```
