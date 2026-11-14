#!/usr/bin/env bash
set -Eeuo pipefail

rules_dir="k8s/observability/slo"
mkdir -p "$rules_dir"

if command -v sloth >/dev/null 2>&1; then
  sloth validate -i observability/slo/cinema-slos.yaml
  sloth generate -i observability/slo/cinema-slos.yaml -o "$rules_dir/rules.yaml"
else
  docker run --rm -v "$PWD:/workspace" -w /workspace ghcr.io/slok/sloth:v0.16.0 validate -i observability/slo/cinema-slos.yaml
  docker run --rm -v "$PWD:/workspace" -w /workspace ghcr.io/slok/sloth:v0.16.0 generate -i observability/slo/cinema-slos.yaml -o "/workspace/$rules_dir/rules.yaml"
fi

kubectl apply -k k8s/observability --prune -l app.kubernetes.io/part-of=cinema-monitoring --prune-allowlist=core/v1/ConfigMap
