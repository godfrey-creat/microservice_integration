#!/usr/bin/env bash
# Builds the image, makes it available to the cluster, applies the manifests
# and waits until everything is ready.
#
# Usage:  ./scripts/deploy.sh [minikube|kind|registry]     (default: minikube)
#         REGISTRY=docker.io/<user> ./scripts/deploy.sh registry
set -euo pipefail

TARGET="${1:-minikube}"
IMAGE="country-info-service"
TAG="${TAG:-1.0.0}"
NAMESPACE="country-info"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo ">> Building image ${IMAGE}:${TAG}"
docker build -t "${IMAGE}:${TAG}" "${ROOT_DIR}"

case "${TARGET}" in
  minikube)
    echo ">> Loading image into minikube"
    minikube image load "${IMAGE}:${TAG}"
    ;;
  kind)
    echo ">> Loading image into kind"
    kind load docker-image "${IMAGE}:${TAG}"
    ;;
  registry)
    : "${REGISTRY:?Set REGISTRY, e.g. REGISTRY=docker.io/yourname}"
    docker tag "${IMAGE}:${TAG}" "${REGISTRY}/${IMAGE}:${TAG}"
    docker push "${REGISTRY}/${IMAGE}:${TAG}"
    (cd "${ROOT_DIR}/k8s" && kustomize edit set image "${IMAGE}=${REGISTRY}/${IMAGE}:${TAG}")
    ;;
  *)
    echo "Unknown target '${TARGET}'. Use minikube, kind or registry." >&2
    exit 1
    ;;
esac

echo ">> Applying manifests"
kubectl apply -k "${ROOT_DIR}/k8s"

echo ">> Waiting for MySQL"
kubectl -n "${NAMESPACE}" rollout status statefulset/mysql --timeout=300s

echo ">> Waiting for the application"
kubectl -n "${NAMESPACE}" rollout status deployment/country-info-service --timeout=300s

kubectl -n "${NAMESPACE}" get pods,svc,hpa,ingress
echo
echo ">> Done. Quick test:"
echo "   kubectl -n ${NAMESPACE} port-forward svc/country-info-service 8080:80"
echo "   curl -X POST localhost:8080/api/v1/countries -H 'Content-Type: application/json' -d '{\"name\":\"kenya\"}'"
