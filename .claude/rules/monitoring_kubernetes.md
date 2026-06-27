# Monitoring on Kubernetes Rules

## Namespace Isolation
- Deploy Prometheus and Grafana in a dedicated `monitoring` namespace — never alongside application services.
- Apply Kubernetes resource quotas (CPU/memory limits) to the `monitoring` namespace.

## Data Persistence
- Back Prometheus with a PersistentVolume so historical metrics survive pod restarts.
- Back Grafana with a PersistentVolume so dashboards and data-source config survive restarts.

## Service Discovery
- Label every service and pod consistently to enable automatic Prometheus discovery:
  ```yaml
  app: <service-name>
  monitoring: "true"
  ```
- Configure PrometheusSelector to target `monitoring: "true"` labels.

## Prometheus Operator
- Use `ServiceMonitor` and `PodMonitor` CRDs to declare what Prometheus scrapes — never hardcode targets.
- These objects auto-adapt when deployments scale or roll.

## Dashboards as Code
- Store all Grafana dashboards as version-controlled JSON files.
- Provision dashboards and data sources via Helm values or ConfigMaps — no manual UI configuration in any environment.

## Version Visibility
- Expose the running application version as a custom Prometheus metric (e.g., `app_info{version="1.2.3"} 1`).
- Visualize it in Grafana to correlate releases with performance changes.

## Alerting
- Define actionable alert rules via `PrometheusRule` CRDs. Minimum required alerts:
  - High error rate
  - Pod restart count > 5
  - Slow response time (p95 latency threshold)
- Route alerts through Alertmanager to at least one channel (Slack, email, or PagerDuty).

## Access Control (production)
- Protect Prometheus and Grafana UIs with authentication (e.g., Rancher SSO or OAuth2 proxy).
- Enforce TLS on all monitoring endpoints.
- Apply RBAC to restrict who can view or modify dashboards and alert rules.
