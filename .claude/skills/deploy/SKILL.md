# /deploy — Validate and apply Kubernetes deployment

Validate Kubernetes manifests or Helm charts and apply them to the target cluster.

## Usage

```
/deploy [environment]
```

Environment: `local` (Rancher / k3d), `azure`, or `dry-run` (default — validate only, do not apply).

## Steps

### 1. Validate manifests

```sh
# Lint all YAML files
find infra/k8s -name '*.yaml' | xargs kubectl apply --dry-run=client -f

# If using Helm
helm lint infra/helm/jobready
helm template infra/helm/jobready | kubectl apply --dry-run=client -f -
```

### 2. Check for hardcoded secrets

Scan for any base64-encoded secrets or literal passwords in manifests:
```sh
grep -r 'password\|secret\|token\|apikey' infra/k8s/ --include='*.yaml' -i
```
If any are found, stop and report them — they must be moved to Kubernetes Secrets or sealed secrets.

### 3. Verify environment-specific config

- All environment-dependent values must come from ConfigMaps or Secrets, not hardcoded in `Deployment` specs.
- Check that image tags are explicit (not `latest`) in production manifests.
- Confirm resource `requests` and `limits` are set on each container.

### 4. Apply (if environment is not `dry-run`)

```sh
# Rancher / local
kubectl apply -f infra/k8s/ --namespace jobready

# Azure (ensure correct context is active first)
kubectl config use-context <azure-context>
kubectl apply -f infra/k8s/ --namespace jobready
```

### 5. Verify rollout

```sh
kubectl rollout status deployment/<service> -n jobready
kubectl get pods -n jobready
```

Check that all pods reach `Running` and pass their readiness probes.

## Rules

- Never deploy directly from a feature branch — only from `main` (CI/CD handles this automatically on merge).
- Never hardcode credentials, image registry passwords, or API keys in manifests.
- If a new service is being deployed for the first time, ensure its Secret and ConfigMap are created before applying the Deployment.
- `pgadmin` is a dev-only service — exclude it from all Kubernetes manifests.
