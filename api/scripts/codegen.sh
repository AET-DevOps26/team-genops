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
npx @redocly/cli lint "$SPEC"

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
  out="$SERVICES/$svc"

  echo "==> Generating Spring stubs for $svc (tag: $tag) -> $out/src/main/java/com/jobready/$svc/generated"
  npx @openapitools/openapi-generator-cli generate \
    -i "$SPEC" \
    -g spring \
    -o "$out" \
    --global-property "apis=$tag,models,supportingFiles=ApiUtil.java" \
    --additional-properties "useSpringBoot3=true,interfaceOnly=true,\
useTags=true,openApiNullable=false,hideGenerationTimestamp=true,\
basePackage=com.jobready.$svc,apiPackage=com.jobready.$svc.generated.api,\
modelPackage=com.jobready.$svc.generated.modelDto"
done



# ---------------------------------------------------------------------------
# Web client — TypeScript types
# ---------------------------------------------------------------------------
echo "==> Generating TypeScript types -> $WEB_CLIENT/src/generated/openapi.ts"
npx openapi-typescript "$SPEC" -o "$WEB_CLIENT/src/generated/openapi.ts"

echo ""
echo "Codegen complete. Review changes with:"
echo "  git diff -- $SERVICES $WEB_CLIENT/src/generated/openapi.ts"
