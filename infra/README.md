# JobReady Deployment

Two environments, one Helm chart:

| | dev | prod |
|---|---|---|
| Platform | Azure **AKS** (Terraform-provisioned) | TUM **Rancher** cluster `stud` |
| Namespace | `jobready` (chart creates it) | `ge86yog-devops-genops` (pre-existing) |
| Ingress + TLS | Ansible installs ingress-nginx + cert-manager | platform-provided |
| Hostname | `<label>.<region>.cloudapp.azure.com` (reserved IP) | `*.stud.k8s.aet.cit.tum.de` |

**Ownership:** Terraform = Azure cloud only. Ansible = all Kubernetes config + Helm
deploys (both envs). Helm is invoked **only** through Ansible. GitHub Actions builds
images (CI) and runs Terraform/Ansible (CD).

```
infra/
├── terraform/        # Azure: RG, AKS, reserved ingress IP (dev only)
├── ansible/          # bootstrap (dev add-ons) + deploy (both envs)
└── helm/jobready/    # the chart + values-dev.yaml / values-prod.yaml
```

---

## ⚠️ Confirm these facts before deploying

In `infra/helm/jobready/values-prod.yaml`:

- `ingress.host` — exact hostname under `*.stud.k8s.aet.cit.tum.de`
- `ingress.ingressClassName` — the platform ingress class (Rancher UI → Ingresses)
- `ingress.clusterIssuer` — the **production** cert-manager ClusterIssuer (NOT staging;
  a staging cert is untrusted and silently breaks the `Secure` auth cookies)

You can't list `clusterissuers`/`ingressclasses` with a student token — get these from
the course docs, the Rancher UI's Ingress-create form, or a working app's annotations.

---

## Generate the JWT signing keys (once)

Auth needs stable RSA keys, or it generates ephemeral ones that die on every restart.

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem  # PKCS#8
openssl rsa -in private.pem -pubout -out public.pem                            # X.509
```

---

## Manual prod deploy (the tracer bullet — slice 3)

Validates the whole prod path by hand before automating.

```sh
export KUBECONFIG=~/.kube/stud.yaml
kubectl config use-context stud
NS=ge86yog-devops-genops

# 1. Secrets
kubectl create secret generic postgres-secret -n $NS \
  --from-literal=POSTGRES_USER=jobready \
  --from-literal=POSTGRES_PASSWORD='<pick-one>' \
  --from-literal=POSTGRES_DB=jobready

kubectl create secret generic auth-jwt -n $NS \
  --from-file=JWT_PRIVATE_KEY=private.pem \
  --from-file=JWT_PUBLIC_KEY=public.pem

kubectl create secret docker-registry ghcr-pull -n $NS \
  --docker-server=ghcr.io \
  --docker-username=<github-user> \
  --docker-password=<PAT-with-read:packages>

# 2. Deploy (fill the TODOs in values-prod.yaml first)
helm upgrade --install jobready infra/helm/jobready \
  -n $NS -f infra/helm/jobready/values-prod.yaml

# 3. Verify
kubectl get pods,ingress -n $NS
# then open https://<your host>  → app loads, login works (trusted padlock)
```

Once this works, switch to the Ansible flow below — it does the same thing reproducibly.

---

## Ansible deploy (reproducible)

```sh
cd infra/ansible
ansible-galaxy collection install -r requirements.yml   # needs: ansible-core, helm, `pip install kubernetes`

# Put secrets in an encrypted vault per env:
cp group_vars/vault.example.yml inventories/prod/group_vars/vault.yml
ansible-vault encrypt inventories/prod/group_vars/vault.yml

# prod (TUM already has ingress+cert-manager, so bootstrap is a no-op):
KUBECONFIG=~/.kube/stud.yaml \
  ansible-playbook -i inventories/prod/hosts.yml playbooks/deploy.yml --ask-vault-pass

# dev (AKS): provision first, then bootstrap + deploy
cd ../terraform && terraform init -backend=false && terraform apply   # see versions.tf for remote state
#   note the outputs: ingress_ip, ingress_fqdn  (put the fqdn in values-dev.yaml host)
cd ../ansible
KUBECONFIG=<aks-kubeconfig> ansible-playbook -i inventories/dev/hosts.yml \
  playbooks/bootstrap.yml -e ingress_ip=<ingress_ip> -e azure_lb_resource_group=rg-jobready-dev --ask-vault-pass
KUBECONFIG=<aks-kubeconfig> ansible-playbook -i inventories/dev/hosts.yml \
  playbooks/deploy.yml --ask-vault-pass
```

---

## CI/CD

- **CI** (`ci-*.yml`): build + test each service on PRs.
- **Build & Push Images** (`build-images.yml`): on merge to main / tags → pushes
  `ghcr.io/aet-devops26/jobready-{auth,web-client,gateway}` tagged `sha-<short>` + `latest`.
- **CD - Dev** (`cd-dev.yml`): after images publish → `terraform apply` → Ansible bootstrap + deploy.
- **CD - Prod** (`cd-prod.yml`): on GitHub Release (manual-approval `production` environment) → Ansible deploy.

### Required GitHub secrets

| Secret | Used by | What |
|---|---|---|
| `AZURE_CLIENT_ID` / `AZURE_TENANT_ID` / `AZURE_SUBSCRIPTION_ID` | cd-dev | Azure OIDC login |
| `TFSTATE_RG` / `TFSTATE_SA` | cd-dev | Terraform remote-state storage |
| `RANCHER_KUBECONFIG` | cd-prod | TUM kubeconfig (note: token expires — refresh periodically) |
| `ANSIBLE_VAULT_PASSWORD` | cd-dev, cd-prod | decrypts the committed `vault.yml` |

> GHCR images default to **private** → the `ghcr-pull` secret must exist in each
> namespace (Ansible creates it from the vault). Either keep it, or make the
> packages public to drop the pull secret entirely.
