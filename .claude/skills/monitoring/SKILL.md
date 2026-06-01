# /monitoring — Prometheus and Grafana configuration

Generate or validate Prometheus scrape config, alert rules, and Grafana dashboard JSON.

## Usage

```
/monitoring [task]
```

Tasks: `validate`, `alert <service>`, `dashboard <service>`, `check-metrics <service>`. No argument → validate existing config.

## Required metrics (per project requirements)

Every Spring Boot service and the GenAI service must expose at minimum:
- **Request count** — total requests per endpoint
- **Latency** — p50, p95, p99 response time
- **Error rate** — 4xx and 5xx responses

Spring Boot Actuator + Micrometer exposes these automatically at `/actuator/prometheus`.
FastAPI requires `prometheus-fastapi-instrumentator`.

## Validate existing config

```sh
# Check Prometheus config syntax
docker run --rm -v $(pwd)/monitoring:/etc/prometheus \
  prom/prometheus promtool check config /etc/prometheus/prometheus.yml

# Check alert rules
docker run --rm -v $(pwd)/monitoring:/etc/prometheus \
  prom/prometheus promtool check rules /etc/prometheus/alerts/*.yml
```

## Generate a Prometheus scrape config entry

For each new service, add a scrape job to `monitoring/prometheus.yml`:

```yaml
- job_name: '<service>'
  static_configs:
    - targets: ['<service>:8080']   # or :8000 for genai
  metrics_path: '/actuator/prometheus'   # Spring Boot
  # metrics_path: '/metrics'            # FastAPI
```

## Generate an alert rule

Minimum required alert: service down. Template for `monitoring/alerts/<service>.yml`:

```yaml
groups:
  - name: <service>
    rules:
      - alert: <Service>Down
        expr: up{job="<service>"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "<service> is down"
          description: "No scrape data from <service> for more than 1 minute."

      - alert: <Service>HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{job="<service>",status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count{job="<service>"}[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on <service>"
          description: "More than 5% of requests are failing."
```

## Grafana dashboards

- Dashboard JSON files live in `monitoring/grafana/dashboards/`.
- Export dashboards from Grafana UI (Dashboard → Share → Export → Save to file).
- Commit the exported JSON — dashboards are code.
- Each dashboard must include panels for: request rate, error rate, p95 latency, and pod health.

## Rules

- Every new service added to docker-compose must also be added to `prometheus.yml`.
- Alert rules must be real and linked to system behaviour — not placeholder alerts that never fire.
- Dashboard JSON files must be committed to the repo — never leave dashboards only in a running Grafana instance.
