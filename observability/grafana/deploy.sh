#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

for command in helm kubectl; do
  if ! command -v "$command" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$command" >&2
    exit 1
  fi
done

if ! kubectl get namespace monitoring >/dev/null 2>&1; then
  kubectl create namespace monitoring
fi

if ! helm repo list | awk 'NR > 1 && $1 == "grafana-community" { found = 1 } END { exit !found }'; then
  helm repo add grafana-community https://grafana-community.github.io/helm-charts
fi

helm repo update grafana-community

if ! kubectl get secret grafana-admin --namespace monitoring >/dev/null 2>&1; then
  if [[ -z "${GRAFANA_ADMIN_PASSWORD:-}" ]]; then
    if [[ ! -t 0 ]]; then
      printf 'Set GRAFANA_ADMIN_PASSWORD or run this script in an interactive terminal.\n' >&2
      exit 1
    fi
    read -r -s -p 'Grafana admin password: ' GRAFANA_ADMIN_PASSWORD
    printf '\n'
  fi

  if [[ -z "$GRAFANA_ADMIN_PASSWORD" ]]; then
    printf 'Grafana admin password cannot be empty.\n' >&2
    exit 1
  fi

  secret_file="$(mktemp)"
  trap 'rm -f "$secret_file"' EXIT
  printf '%s' "$GRAFANA_ADMIN_PASSWORD" > "$secret_file"
  unset GRAFANA_ADMIN_PASSWORD

  kubectl create secret generic grafana-admin \
    --namespace monitoring \
    --from-literal=admin-user=admin \
    --from-file=admin-password="$secret_file" \
    --dry-run=client \
    -o yaml | kubectl apply -f -
fi

kubectl apply -k k8s/observability/monitoring

helm upgrade --install grafana grafana-community/grafana \
  --namespace monitoring \
  --values observability/grafana/values.yaml \
  --wait \
  --timeout 5m

kubectl rollout status deployment/grafana --namespace monitoring
