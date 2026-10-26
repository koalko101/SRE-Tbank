# Minikube

Нужны запущенные Docker, Minikube и kubectl. Команды выполняются из корня проекта в zsh:

```zsh
minikube start
eval "$(minikube -p minikube docker-env)"
helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo update
helm upgrade --install vm-single vm/victoria-metrics-single \
  --version 0.49.0 \
  --namespace monitoring \
  --create-namespace \
  --values k8s/monitoring/victoria-metrics.yaml

docker build -t cinema-backend:latest .
docker build -t cinema-frontend:latest ./frontend
kubectl apply -f k8s/namespace.yaml
kubectl apply -n cinema -f k8s/
kubectl -n cinema rollout status deployment/backend
kubectl -n cinema rollout status deployment/frontend
kubectl -n cinema get pods,services
kubectl -n monitoring rollout status statefulset/vm-single-victoria-metrics-single-server
kubectl -n monitoring get pods,services
kubectl apply -f k8s/observability/
kubectl -n logging get pods,services,pvc
minikube service frontend -n cinema
```

Чарт `victoria-metrics-single` разворачивает VictoriaMetrics Single. Конфигурация
скрейпинга из `monitoring/victoria-metrics.yaml` использует Kubernetes service
discovery и собирает pod-ы с аннотациями `prometheus.io/scrape`, включая backend
и Postgres exporter. Версия чарта зафиксирована для повторяемой установки.

Проверить поступление метрик можно через port-forward и API VictoriaMetrics:

```zsh
kubectl -n monitoring port-forward svc/vm-single-victoria-metrics-single-server 8428:8428
curl "http://localhost:8428/api/v1/query?query=up"
curl "http://localhost:8428/api/v1/label/__name__/values"
```

Для открытия ```vmui``` нужно сделать port-forward:
```zsh
./k8s/observability/vmui.sh
```

Для роли администратора зарегистрируйте пользователя, укажите его email в нижнем регистре в `ADMIN_EMAIL` в `k8s/config.yaml`, затем примените конфигурацию и перезапустите backend:

```zsh
kubectl apply -f k8s/config.yaml
kubectl -n cinema rollout restart deployment/backend
```

Масштабирование backend для демонстрации:

```zsh
kubectl -n cinema scale deployment/backend --replicas=3
kubectl -n cinema get pods -w
```
