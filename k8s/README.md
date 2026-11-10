# Minikube

Нужны запущенные Docker, Minikube и kubectl. Команды выполняются из корня проекта в zsh:

```zsh
minikube start
eval "$(minikube -p minikube docker-env)"
docker build -t cinema-backend:latest .
docker build -t cinema-frontend:latest ./frontend
kubectl apply -f k8s/namespace.yaml
kubectl apply -n cinema -f k8s/
kubectl -n cinema rollout status deployment/backend
kubectl -n cinema rollout status deployment/frontend
kubectl -n cinema get pods,services
helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install vm-single vm/victoria-metrics-single --version 0.49.0 --namespace monitoring --create-namespace --values observability/monitoring/victoria-metrics.yaml
helm upgrade --install blackbox-exporter prometheus-community/prometheus-blackbox-exporter -n monitoring --create-namespace -f observability/blackbox-exporter.yaml
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

## SLI/SLO/SLA и Grafana

После сборки новой версии backend обновите приложение и Blackbox Exporter:

```zsh
eval "$(minikube -p minikube docker-env)"
docker build -t cinema-backend:latest .
kubectl -n cinema rollout restart deployment/backend
kubectl -n cinema rollout status deployment/backend
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update
helm upgrade --install blackbox-exporter prometheus-community/prometheus-blackbox-exporter --namespace monitoring --create-namespace --values observability/blackbox-exporter.yaml
```

Создайте Secret для входа в Grafana, задав собственный пароль:

```zsh
kubectl create secret generic grafana-admin -n monitoring --from-literal=admin-user=admin --from-literal=admin-password='your-local-passwd' --dry-run=client -o yaml | kubectl apply -f -
```

Установите Grafana и подключите VictoriaMetrics как источник данных:

```zsh
helm repo add grafana-community https://grafana-community.github.io/helm-charts --force-update
helm repo update
helm upgrade --install grafana grafana-community/grafana --namespace monitoring --create-namespace --values observability/grafana/values.yaml
kubectl create configmap cinema-slo-dashboard -n monitoring --from-file=cinema-slo.json=observability/grafana/cinema-slo.json --dry-run=client -o yaml | kubectl apply -f -
kubectl label configmap cinema-slo-dashboard -n monitoring grafana_dashboard=1 --overwrite
```

Сгенерируйте Sloth recording rules и запустите vmalert для их вычисления:

```zsh
bash observability/slo/deploy-rules.sh
```

Откройте Grafana:

```zsh
kubectl -n monitoring rollout status deployment/grafana
kubectl -n monitoring port-forward svc/grafana 3000:80
```

Панель `Cinema SLI SLO SLA` появится автоматически. Войдите как `admin`, используя пароль из Secret. Значения SLI/SLO появятся после поступления трафика. Для демонстрации продолжайте обращаться к каталогу во время недоступности PostgreSQL:

```zsh
kubectl -n cinema scale deployment/postgres --replicas=0
kubectl -n cinema scale deployment/postgres --replicas=1
```

Синтетические запросы Blackbox Exporter помечаются специальным заголовком и не учитываются в пользовательских запросных SLI. Их результат отображается отдельно через `probe_success`.
