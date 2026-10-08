#!/usr/bin/env bash
# Removes everything the deploy script created, including the MySQL volume.
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
kubectl delete -k "${ROOT_DIR}/k8s" --ignore-not-found
kubectl -n country-info delete pvc --all --ignore-not-found 2>/dev/null || true
echo ">> Removed"
