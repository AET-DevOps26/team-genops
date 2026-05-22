#!/usr/bin/env bash
# Regenerate every artefact downstream of openapi.yaml.
# Run from anywhere: the script always cd's to its own directory first.
set -euo pipefail

cd "$(dirname "$0")"            # cwd: api/scripts/
SPEC=../openapi.yaml
ROOT=../..
SERVICES="$ROOT/services"
WEB_CLIENT="$ROOT/web-client"

echo "==> Linting $SPEC"
npx --yes @redocly/cli lint "$SPEC"

# ---------------------------------------------------------------------------
# Spring Boot services
# To add a new service: append a line with its directory name and OpenAPI tag.
# The tag must match the `tags: [TagName]` value used in openapi.yaml.
# ---------------------------------------------------------------------------
declare -A SPRING_SERVICES=(
  [auth]="Auth"
  # [profile]="Profile"
  # [application]="Applications"
  # [email]="Email"
  # [document]="Documents"
)

for svc in "${!SPRING_SERVICES[@]}"; do
  tag="${SPRING_SERVICES[$svc]}"
  out="$SERVICES/$svc/src/main/java/com/jobready/$svc/generated"

  echo "==> Generating Spring stubs for $svc (tag: $tag) -> $out"
  npx --yes @openapitools/openapi-generator-cli generate \
    -i "$SPEC" \
    -g spring \
    -o "$out" \
    --global-property "apis=$tag,models" \
    --additional-properties "useSpringBoot3=true,interfaceOnly=true,useTags=true,basePackage=com.jobready.$svc,apiPackage=com.jobready.$svc.generated.api,modelPackage=com.jobready.$svc.generated.model"

done

# ---------------------------------------------------------------------------
# GenAI service — Python client (consumes the full API, does not produce it)
# ---------------------------------------------------------------------------
echo "==> Generating Python client for genai -> $SERVICES/genai/client"
if command -v uv &>/dev/null; then
  uvx openapi-python-client generate \
    --path "$SPEC" \
    --output-path "$SERVICES/genai/client" \
    --overwrite
else
  echo "  (uv not available — install from https://docs.astral.sh/uv/ and re-run)"
  exit 1
fi

# ---------------------------------------------------------------------------
# Web client — TypeScript types
# ---------------------------------------------------------------------------
echo "==> Generating TypeScript types -> $WEB_CLIENT/src/api.ts"
npx --yes openapi-typescript "$SPEC" -o "$WEB_CLIENT/src/api.ts"

echo ""
echo "✓ Codegen complete. Review changes with:"
echo "  git diff -- $SERVICES $WEB_CLIENT/src/api.ts"
