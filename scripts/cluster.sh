#!/usr/bin/env bash
# Dev-cluster cost switch: stop deallocates the node VMs (compute billing → 0),
# start resumes the same cluster with all workloads/disks/IP intact.
#
# Usage:
#   RG=<resource-group> CLUSTER=<aks-name> SUBSCRIPTION=<sub-id> \
#     ./scripts/cluster.sh stop|start|status
#
# Set the three env vars first (plain quoted literals — no ${} around values):
#   export RG="<resource-group>"
#   export CLUSTER="<aks-cluster-name>"
#   export SUBSCRIPTION="<subscription-id>"
#
# All three env vars are required — ask a teammate or check the team Key Vault
# for the values. Prereq: az login with an account that has Contributor.
set -euo pipefail

ACTION="${1:-}"

if [[ "$ACTION" != "stop" && "$ACTION" != "start" && "$ACTION" != "status" ]]; then
  echo "usage: RG=... CLUSTER=... SUBSCRIPTION=... $0 stop|start|status" >&2
  exit 2
fi

missing=()
[[ -z "${RG:-}" ]] && missing+=(RG)
[[ -z "${CLUSTER:-}" ]] && missing+=(CLUSTER)
[[ -z "${SUBSCRIPTION:-}" ]] && missing+=(SUBSCRIPTION)
if (( ${#missing[@]} )); then
  echo "error: missing required env var(s): ${missing[*]}" >&2
  echo "hint: values live in the team Key Vault / ask a teammate" >&2
  exit 2
fi

# NOTE: az only accepts flags AFTER the full command (az aks show --subscription ...),
# never between `az`/`aks` and the subcommand.
TARGET=(-g "$RG" -n "$CLUSTER" --subscription "$SUBSCRIPTION")

STATE=$(az aks show "${TARGET[@]}" --query powerState.code -o tsv 2>/dev/null || echo "absent")
echo "Cluster '$CLUSTER' power state: $STATE"

case "$ACTION" in
  status)
    ;;
  stop)
    if [[ "$STATE" == "absent" ]]; then echo "Cluster does not exist — nothing to do."; exit 0; fi
    if [[ "$STATE" == "Stopped" ]]; then echo "Already stopped."; exit 0; fi
    az aks stop "${TARGET[@]}"
    echo "Stopped — node VMs deallocated, compute billing paused."
    ;;
  start)
    if [[ "$STATE" == "absent" ]]; then echo "Cluster does not exist — run CD - Dev to create it."; exit 1; fi
    if [[ "$STATE" == "Running" ]]; then echo "Already running."; exit 0; fi
    az aks start "${TARGET[@]}"
    echo "Started — app should be back at the usual URL in a few minutes."
    ;;
esac
