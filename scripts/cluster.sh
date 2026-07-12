#!/usr/bin/env bash
# Dev-cluster cost switch: stop deallocates the node VMs (compute billing → 0),
# start resumes the same cluster with all workloads/disks/IP intact.
#
# Usage:
#   ./scripts/cluster.sh stop|start|status
#
# Prereqs: az login (any team member with Contributor on the subscription).
# Overrides via env: RG, CLUSTER, SUBSCRIPTION.
set -euo pipefail

ACTION="${1:-}"
RG="${RG:-rg-jobready-dev}"
CLUSTER="${CLUSTER:-aks-jobready-dev}"
SUBSCRIPTION="${SUBSCRIPTION:-01a8cd8b-b0c9-4f88-8ef2-1222441bd9d4}"   # team dev sub

if [[ "$ACTION" != "stop" && "$ACTION" != "start" && "$ACTION" != "status" ]]; then
  echo "usage: $0 stop|start|status" >&2
  exit 2
fi

AZ="az --subscription $SUBSCRIPTION"

STATE=$($AZ aks show -g "$RG" -n "$CLUSTER" --query powerState.code -o tsv 2>/dev/null || echo "absent")
echo "Cluster '$CLUSTER' power state: $STATE"

case "$ACTION" in
  status)
    ;;
  stop)
    if [[ "$STATE" == "absent" ]]; then echo "Cluster does not exist — nothing to do."; exit 0; fi
    if [[ "$STATE" == "Stopped" ]]; then echo "Already stopped."; exit 0; fi
    $AZ aks stop -g "$RG" -n "$CLUSTER"
    echo "Stopped — node VMs deallocated, compute billing paused."
    ;;
  start)
    if [[ "$STATE" == "absent" ]]; then echo "Cluster does not exist — run CD - Dev to create it."; exit 1; fi
    if [[ "$STATE" == "Running" ]]; then echo "Already running."; exit 0; fi
    $AZ aks start -g "$RG" -n "$CLUSTER"
    echo "Started — app should be back at the usual URL in a few minutes."
    ;;
esac
