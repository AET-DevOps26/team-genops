#!/usr/bin/env python3
"""Seed Langfuse with custom model prices (as code).

Reads ``models.json`` next to this file and registers each model's per-token
prices via the Langfuse public API, so token cost shows up in the dashboards.
OpenRouter models aren't in Langfuse's built-in price list, so without this step
you'd see token counts but $0 cost.

Idempotent: a model whose ``matchPattern`` is already registered is skipped, so
this is safe to run repeatedly.

Run it once after the Langfuse stack is up:

    python3 monitoring/langfuse/seed_models.py

Credentials (LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY) are read from the
environment; if unset, the repo-root .env is loaded automatically. The API host
defaults to http://localhost:3000 (the published port) — override with
LANGFUSE_SEED_HOST if needed. Note this is the *host* URL, not the in-container
LANGFUSE_HOST (langfuse-web:3000), which only resolves inside the compose network.
"""

import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parent.parent
ENV_FILE = REPO_ROOT / ".env"


def load_env_file(path: Path) -> None:
    """Populate os.environ from a .env file without overriding existing vars."""
    if not path.exists():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip())


def api(method: str, url: str, auth: str, body: dict | None = None) -> dict:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Basic {auth}")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read() or "{}")


def existing_model_names(host: str, auth: str) -> set[str]:
    """All model names already in the project. Paginates — the list includes
    hundreds of Langfuse built-ins, so the custom models can be on later pages."""
    names: set[str] = set()
    page = 1
    while True:
        resp = api("GET", f"{host}/api/public/models?limit=100&page={page}", auth)
        for m in resp.get("data", []):
            names.add(m.get("modelName"))
        meta = resp.get("meta", {})
        if page >= meta.get("totalPages", page):
            break
        page += 1
    return names


def main() -> int:
    load_env_file(ENV_FILE)
    host = os.environ.get("LANGFUSE_SEED_HOST", "http://localhost:3000").rstrip("/")
    pk = os.environ.get("LANGFUSE_PUBLIC_KEY")
    sk = os.environ.get("LANGFUSE_SECRET_KEY")
    if not pk or not sk:
        print(
            "LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY not set (env or repo .env).",
            file=sys.stderr,
        )
        return 1

    auth = base64.b64encode(f"{pk}:{sk}".encode()).decode()
    models = json.loads((HERE / "models.json").read_text())

    existing = existing_model_names(host, auth)

    for model in models:
        if model["modelName"] in existing:
            print(f"= {model['modelName']}: price already registered, skipping")
            continue
        try:
            api("POST", f"{host}/api/public/models", auth, model)
        except urllib.error.HTTPError as e:
            # Registered concurrently or by a prior run — treat as already-seeded.
            if e.code == 400 and b"already exists" in e.read():
                print(f"= {model['modelName']}: already exists, skipping")
                continue
            raise
        print(f"+ {model['modelName']}: registered (in ${model['inputPrice'] * 1e6:.4f}/1M, out ${model['outputPrice'] * 1e6:.4f}/1M)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except urllib.error.HTTPError as e:
        print(f"Langfuse API error {e.code}: {e.read().decode()}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(
            f"Cannot reach Langfuse at {os.environ.get('LANGFUSE_SEED_HOST', 'http://localhost:3000')} — is the monitoring stack up? ({e})",
            file=sys.stderr,
        )
        sys.exit(1)
