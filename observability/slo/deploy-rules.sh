#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p observability/slo/generated

if command -v sloth >/dev/null 2>&1; then
  sloth validate -i observability/slo/cinema-slos.yaml
  sloth generate -i observability/slo/cinema-slos.yaml -o observability/slo/generated/rules.yaml
else
  docker run --rm -v "$PWD:/workspace" -w /workspace ghcr.io/slok/sloth:v0.16.0 validate -i observability/slo/cinema-slos.yaml
  docker run --rm -v "$PWD:/workspace" -w /workspace ghcr.io/slok/sloth:v0.16.0 generate -i observability/slo/cinema-slos.yaml -o observability/slo/generated/rules.yaml
fi

kubectl create configmap cinema-slo-rules -n monitoring --from-file=rules.yaml=observability/slo/generated/rules.yaml --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/observability/vmalert-slo.yaml
kubectl -n monitoring rollout restart deployment/vmalert-slo
kubectl -n monitoring rollout status deployment/vmalert-slo
