#!/usr/bin/env bash
# Dev-cluster cost switch: stop deallocates the node VMs (compute billing → 0),
# start resumes the same cluster with all workloads/disks/IP intact.
#
# Usage:
#   RG=<resource-group> CLUSTER=<aks-name> SUBSCRIPTION=<sub-id> \
#     ./scripts/cluster.sh stop|start|status
#
# SUBSCRIPTION is required (plain quoted literal — no ${} around the value):
#   export SUBSCRIPTION="<subscription-id>"        # team Key Vault / teammate
#
# RG and CLUSTER are optional: when unset, the cluster is auto-discovered by
# its Terraform tags (project=jobready, env=dev). Export them only to target
# a different cluster. Prereq: az login with an account that has Contributor.
set -euo pipefail

ACTION="${1:-}"

if [[ "$ACTION" != "stop" && "$ACTION" != "start" && "$ACTION" != "status" ]]; then
  echo "usage: SUBSCRIPTION=... [RG=... CLUSTER=...] $0 stop|start|status" >&2
  exit 2
fi

if [[ -z "${SUBSCRIPTION:-}" ]]; then
  echo "error: SUBSCRIPTION env var is required" >&2
  echo "hint: the value lives in the team Key Vault / ask a teammate" >&2
  exit 2
fi

if [[ -z "${RG:-}" || -z "${CLUSTER:-}" ]]; then
  echo "RG/CLUSTER not set — discovering by tags (project=jobready, env=dev)..."
  FOUND=$(az aks list --subscription "$SUBSCRIPTION" \
    --query "[?tags.project=='jobready' && tags.env=='dev'].[resourceGroup,name]" -o tsv)
  if [[ -z "$FOUND" ]]; then
    echo "Cluster does not exist — nothing to do."
    # start on a non-existent cluster is an error; stop/status are no-ops.
    [[ "$ACTION" == "start" ]] && exit 1 || exit 0
  fi
  if [[ $(wc -l <<<"$FOUND") -ne 1 ]]; then
    echo "error: multiple clusters match the tags — set RG and CLUSTER explicitly:" >&2
    echo "$FOUND" >&2
    exit 2
  fi
  RG=$(cut -f1 <<<"$FOUND")
  CLUSTER=$(cut -f2 <<<"$FOUND")
  echo "Found cluster '$CLUSTER' in resource group '$RG'."
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
