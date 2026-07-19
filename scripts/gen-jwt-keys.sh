#!/usr/bin/env bash
# Generate the RSA-2048 keypair the auth service signs JWTs with (RS256).
# Prints .env-ready single-line values (newlines escaped as \n, which the
# auth service unescapes on load). The service refuses to start without them.
#
# Usage:
#   ./scripts/gen-jwt-keys.sh          # print both values; paste into .env
#   ./scripts/gen-jwt-keys.sh >> .env  # append directly
set -euo pipefail

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$tmp/private.pem" 2>/dev/null
openssl pkey -in "$tmp/private.pem" -pubout -out "$tmp/public.pem"

escape() { awk '{printf "%s\\n", $0}' "$1"; }

echo "JWT_PRIVATE_KEY=$(escape "$tmp/private.pem")"
echo "JWT_PUBLIC_KEY=$(escape "$tmp/public.pem")"
