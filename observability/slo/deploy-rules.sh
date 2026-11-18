#!/usr/bin/env bash
set -Eeuo pipefail

slo_file="observability/slo/cinema-slos.yaml"
rules_out="k8s/observability/slo/rules.yaml"
alerts_out="k8s/observability/alerting/config/slo-alerts.yaml"

mkdir -p "$(dirname "$rules_out")" "$(dirname "$alerts_out")"

if ! command -v sloth >/dev/null 2>&1; then
  sloth() {
    docker run --rm --user "$(id -u):$(id -g)" -v "$(pwd -P):/workspace" -w /workspace ghcr.io/slok/sloth:v0.16.0 "$@"
  }
fi

sloth validate -i "$slo_file"
sloth generate -i "$slo_file" -o "$rules_out"  --disable-alerts
sloth generate -i "$slo_file" -o "$alerts_out" --disable-recordings

kubectl apply -k k8s/observability --prune -l app.kubernetes.io/part-of=cinema-monitoring --prune-allowlist=core/v1/ConfigMap

kubectl -n monitoring rollout status deployment/vmalert-slo
kubectl -n monitoring rollout status deployment/vmalert-metrics
kubectl -n logging    rollout status deployment/vmalert