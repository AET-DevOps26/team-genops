# JobReady Deployment

Two environments, one Helm chart:

| | dev | prod |
|---|---|---|
| Platform | Azure **AKS** (Terraform-provisioned) | TUM **Rancher** cluster `stud` |
| Namespace | `jobready` (Ansible creates it) | `ge86yog-devops-genops` (pre-existing) |
| Ingress + TLS | Ansible installs ingress-nginx + cert-manager | platform-provided |
| Hostname | `<label>.<region>.cloudapp.azure.com` (reserved IP) | `*.stud.k8s.aet.cit.tum.de` |

**Ownership:** Terraform = Azure cloud only. Ansible = all Kubernetes config + Helm
deploys (both envs). Helm is invoked **only** through Ansible. GitHub Actions builds
images (CI) and runs Terraform/Ansible (CD). Architecture + rationale: [`HANDOVER.md`](./HANDOVER.md).

```
infra/
├── terraform/        # Azure: RG, AKS, reserved ingress IP (dev only)
├── ansible/          # bootstrap (dev add-ons) + deploy (both envs)
└── helm/jobready/    # the chart + values-dev.yaml / values-prod.yaml
```

---

# Team secrets — Azure Key Vault

Shared secrets (DB passwords, JWT keys, GHCR tokens, …) live in a team **Azure Key Vault**,
readable from any machine after `az login` — no key files to copy around.

| | |
|---|---|
| Vault | `kv-jobready-dev` — <https://kv-jobready-dev.vault.azure.net/> |
| Location | resource group `rg-jobready-dev`, subscription `185fd5c7-…` ("Azure for Students") |
| Access model | **Vault access policies** (per-person, granted in the Portal) |
| Managed by | **hand (Portal) — deliberately NOT Terraform**, so `terraform destroy`/cluster rebuilds can never take the secrets with it |

### Get access (once per person)

Ask a teammate who already has access: Portal → `kv-jobready-dev` → **Access policies →
+ Create** → Secret permissions **Get, List, Set** → select your `@tum.de` account.
(CLI access works with just the policy; seeing the vault in the Portal additionally needs a
reader/contributor role on `rg-jobready-dev`.)

### Use it (any machine)

```sh
az login                                                  # once per machine
az keyvault secret list --vault-name kv-jobready-dev -o table
az keyvault secret show --vault-name kv-jobready-dev --name <name> --query value -o tsv
az keyvault secret set  --vault-name kv-jobready-dev --name <name> --value '<value>'
```

Multi-line values (PEM keys) go in via file: `az keyvault secret set … --file private.pem`.

**Conventions:** kebab-case names, prefixed by consumer — e.g. `prod-postgres-password`,
`prod-jwt-private-key`, `ghcr-pull-token`. The vault is the source of truth for humans;
deploy-time secrets still flow through the encrypted Ansible vault / GitHub secrets — when
you rotate a value here, update those too.

---

# Bringing the pipeline up

A working deploy reduces to making three invariants true, then automating them:

1. **Images exist** in GHCR (the CI job).
2. **The cluster can pull them and has its secrets** (`ghcr-pull`, `postgres-secret`, `auth-jwt`).
3. **Something applies the chart** (you by hand first; then Ansible via CD).

Do it in order. Prove prod by hand before trusting automation; dev/AKS comes last.
**Shortest path to live prod:** Phase A → B → C.

---

## Phase A — Gather the 3 prod facts

These are the placeholders in `infra/helm/jobready/values-prod.yaml`; a correct ingress
can't render without them.

| Fact | Where to get it | Why it matters |
|---|---|---|
| `ingress.host` | Rancher UI → project → **Ingresses → Create** (shows the wildcard) | Browser address + the name cert-manager issues a cert for |
| `ingress.ingressClassName` | same form, or `kubectl get ingressclass` if allowed | Must match the platform controller, else the ingress is ignored |
| `ingress.clusterIssuer` | course docs, or a **working** app's ingress annotations | Use the **production** issuer — a staging cert is untrusted and silently breaks the `Secure` auth cookies |

Fill those three into `values-prod.yaml`. (Student tokens can't list `clusterissuers`/`ingressclasses`, hence the manual lookup.)

---

## Phase B — Publish images (CI)

The chart pulls `ghcr.io/aet-devops26/jobready-{auth,web-client,gateway}`, which don't
exist until CI runs.

1. **Merge the PR to main.** That push triggers `build-images.yml` (Actions → *Build & Push Images*).
2. Confirm the three packages appear under the org's **Packages**.

`build-images.yml` only fires once it's on the default branch — that's why merging is step 1.

> If the first push fails with a permissions error, allow Actions to publish packages in
> the org's *Packages* settings. The workflow already requests `packages: write`.

---

## Phase C — First manual prod deploy (the tracer bullet)

Proves invariants 2 + 3 by hand, where failures are easy to read.

```sh
export KUBECONFIG=~/.kube/config      # the kubeconfig that holds a working `stud` context
kubectl config use-context stud       # target the prod cluster (NOT fact-checking/AKS)
kubectl config current-context        # sanity: must print  stud
NS=ge86yog-devops-genops

# 1. Stable RSA JWT keys (else auth self-generates ephemeral keys that die on restart)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem   # PKCS#8
openssl rsa -in private.pem -pubout -out public.pem                             # X.509

# 2. The three secrets (names match values.yaml)
kubectl create secret generic postgres-secret -n $NS \
  --from-literal=POSTGRES_USER=jobready \
  --from-literal=POSTGRES_PASSWORD='<choose-a-password>' \
  --from-literal=POSTGRES_DB=jobready

kubectl create secret generic auth-jwt -n $NS \
  --from-file=JWT_PRIVATE_KEY=private.pem --from-file=JWT_PUBLIC_KEY=public.pem

kubectl create secret docker-registry ghcr-pull -n $NS \
  --docker-server=ghcr.io \
  --docker-username=<github-user> \
  --docker-password=<PAT-with-read:packages>     # classic PAT, scope: read:packages

# 3. Deploy (values-prod.yaml filled in Phase A)
helm upgrade --install jobready infra/helm/jobready -n $NS -f infra/helm/jobready/values-prod.yaml

# 4. Verify
kubectl get pods -n $NS -w               # all Running/Ready
kubectl get ingress,certificate -n $NS   # certificate should reach READY=True
# open https://<your host> → app loads, green padlock, login works
```

`ghcr-pull` is required (images are private); `auth-jwt` gives stable keys. If the
`certificate` stays `READY=False`, the `clusterIssuer` name (Phase A) is wrong. **The green
padlock + working login is the real success signal** — Secure cookies need trusted TLS.

---

## Phase D — Turn on prod CD

Make Ansible do Phase C reproducibly.

```sh
# 1. Encrypt the prod vault (same values you used by hand)
cp infra/ansible/group_vars/vault.example.yml infra/ansible/inventories/prod/group_vars/vault.yml
#   edit: postgres creds, ghcr user/token, paste private.pem/public.pem into the JWT fields
ansible-vault encrypt infra/ansible/inventories/prod/group_vars/vault.yml
#   commit the ENCRYPTED file
```

In **GitHub → Settings**:
- **Secrets:** `RANCHER_KUBECONFIG`, `ANSIBLE_VAULT_PASSWORD`. Generate a self-contained,
  stud-only kubeconfig for the secret with:
  ```sh
  kubectl --kubeconfig ~/.kube/config --context stud config view --minify --flatten
  ```
- **Environments → `production`** → add yourself as a **Required reviewer**.

Trigger: create a **GitHub Release** (tag `vX.Y.Z`). `build-images` builds semver images and
`cd-prod` waits for your approval; approve → Ansible deploys.

The approval gate is what serialises the release: both `build-images` and `cd-prod` fire on
the release, and approving only after images finish stops the deploy pulling a missing tag.
**Don't remove it.**

---

## Phase E — Dev environment on Azure (optional / later)

One-time Azure prep:

```sh
# 1. Remote state storage for Terraform
az group create -n rg-tfstate -l westeurope
az storage account create -n <globally-unique-name> -g rg-tfstate --sku Standard_LRS
az storage container create -n tfstate --account-name <name>

# 2. GitHub Actions identity (OIDC — no stored password)
APP_ID=$(az ad app create --display-name jobready-gh-oidc --query appId -o tsv)
az ad sp create --id "$APP_ID"
az role assignment create --assignee "$APP_ID" --role Contributor \
  --scope /subscriptions/<subscription-id>
az ad app federated-credential create --id "$APP_ID" --parameters '{
  "name":"gh-env-dev",
  "issuer":"https://token.actions.githubusercontent.com",
  "subject":"repo:AET-DevOps26/team-genops:environment:dev",
  "audiences":["api://AzureADTokenExchange"]}'
```

> **OIDC gotcha:** `cd-dev` runs with `environment: dev`, so the OIDC token subject is
> `repo:.../environment:dev` — **not** a branch ref. The federated credential's `subject`
> must match exactly (as above) or Azure login fails with `AADSTS70021`.

Then in **GitHub**:
- **Secrets:** `AZURE_CLIENT_ID`=`$APP_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`,
  `TFSTATE_RG`=`rg-tfstate`, `TFSTATE_SA`=`<name>` (plus `ANSIBLE_VAULT_PASSWORD`).
- **Environment `dev`** (no reviewer needed).
- Encrypt `inventories/dev/group_vars/vault.yml`; set a real `acme_email` in
  `inventories/dev/group_vars/all.yml`.

Trigger: runs automatically after the next `build-images` on main, or **Actions → CD - Dev →
Run workflow**. Terraform creates the cluster + reserved IP, the run auto-wires the
`cloudapp.azure.com` host into the deploy, and Ansible installs ingress-nginx + cert-manager
and deploys. Read the dev URL from the Terraform output / run log.

---

## Local Ansible runs (without CD)

```sh
cd infra/ansible
ansible-galaxy collection install -r requirements.yml   # needs ansible-core, helm, `pip install kubernetes`

# prod (TUM already has ingress + cert-manager → bootstrap is a no-op)
kubectl --kubeconfig ~/.kube/config config use-context stud      # once: make stud current
KUBECONFIG=~/.kube/config \
  ansible-playbook -i inventories/prod/hosts.yml playbooks/deploy.yml --ask-vault-pass

# dev (AKS): terraform first, then bootstrap + deploy with the outputs
cd ../terraform && terraform init -backend=false && terraform apply
cd ../ansible
KUBECONFIG=<aks-kubeconfig> ansible-playbook -i inventories/dev/hosts.yml playbooks/bootstrap.yml \
  -e ingress_ip=<terraform output ingress_ip> -e azure_lb_resource_group=<terraform output resource_group_name> --ask-vault-pass
KUBECONFIG=<aks-kubeconfig> ansible-playbook -i inventories/dev/hosts.yml playbooks/deploy.yml \
  -e ingress_host=<terraform output ingress_fqdn> --ask-vault-pass
```

---

## Reference

**Workflows**
- `ci-*.yml` — build + test each service on PRs.
- `build-images.yml` — on merge to main / tags → pushes `jobready-{auth,web-client,gateway}` (`sha-<short>` + `latest` + semver).
- `cd-dev.yml` — after images publish → `terraform apply` → Ansible bootstrap + deploy.
- `cd-prod.yml` — on GitHub Release (manual-approval `production` env) → Ansible deploy.

**GitHub secrets**

| Secret | Used by | What |
|---|---|---|
| `AZURE_CLIENT_ID` / `AZURE_TENANT_ID` / `AZURE_SUBSCRIPTION_ID` | cd-dev | Azure OIDC login |
| `TFSTATE_RG` / `TFSTATE_SA` | cd-dev | Terraform remote-state storage |
| `RANCHER_KUBECONFIG` | cd-prod | TUM kubeconfig (token expires — refresh periodically) |
| `ANSIBLE_VAULT_PASSWORD` | cd-dev, cd-prod | decrypts the committed `vault.yml` |

**GitHub environments**

| Environment | Setting |
|---|---|
| `production` | required reviewer (approval gate) |
| `dev` | none |

> Images are **private** on GHCR → the `ghcr-pull` secret must exist in each namespace
> (Ansible creates it from the vault; by hand in Phase C). Or make the packages public to
> drop the pull secret entirely.
