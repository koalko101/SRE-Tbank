# Запуск приложения в Minikube

## Требования

Установите Docker, Minikube, kubectl, Helm и Bash. Все команды запускаются из корня репозитория.

## Приложение

```bash
minikube start
eval "$(minikube -p minikube docker-env)"
docker build -t cinema-backend:latest .
docker build -t cinema-frontend:latest ./frontend
kubectl apply -f k8s/namespace.yaml
kubectl apply -n cinema -f k8s/
kubectl -n cinema rollout status deployment/backend
kubectl -n cinema rollout status deployment/frontend
kubectl -n cinema get pods,services
```

Чтобы назначить администратора приложения, укажите зарегистрированный email в нижнем регистре в `ADMIN_EMAIL` в `k8s/config.yaml`, затем выполните:

```bash
kubectl apply -f k8s/config.yaml
kubectl -n cinema rollout restart deployment/backend
```

## Мониторинг

Добавьте Helm-репозитории и установите VictoriaMetrics и Blackbox Exporter:

```bash
helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana-community https://grafana-community.github.io/helm-charts
helm repo update
helm upgrade --install vm-single vm/victoria-metrics-single --version 0.49.0 --namespace monitoring --create-namespace --values observability/monitoring/victoria-metrics.yaml
helm upgrade --install blackbox-exporter prometheus-community/prometheus-blackbox-exporter --namespace monitoring --values observability/blackbox-exporter.yaml
kubectl -n monitoring rollout status statefulset/vm-single-victoria-metrics-single-server
kubectl apply -f k8s/observability/
```

Конфигурация VictoriaMetrics в `observability/monitoring/victoria-metrics.yaml` собирает метрики pod-ов с аннотацией `prometheus.io/scrape`, включая backend и Postgres exporter. Blackbox Exporter проверяет доступность каталога через frontend.

Grafana устанавливается вместе с ConfigMap дашборда скриптом:

```bash
bash observability/grafana/deploy.sh
```

При первом запуске скрипт запросит пароль администратора Grafana и создаст Secret `grafana-admin` в namespace `monitoring`. Существующий Secret будет использован повторно. Для автоматизированного запуска пароль можно передать через переменную `GRAFANA_ADMIN_PASSWORD`.

Установите Sloth recording rules и vmalert:

```bash
bash observability/slo/deploy-rules.sh
```

Откройте Grafana:

```bash
kubectl -n monitoring rollout status deployment/grafana
kubectl -n monitoring port-forward svc/grafana 3000:80
```

Перейдите на `http://localhost:3000`, войдите как `admin` с заданным паролем и откройте дашборд `Cinema SLI SLO SLA`. Графики SLI/SLO заполняются по мере поступления запросов к приложению.

Для проверки метрик VictoriaMetrics откройте в отдельном терминале:

```bash
kubectl -n monitoring port-forward svc/vm-single-victoria-metrics-single-server 8428:8428
```

Затем выполните запросы:

```bash
curl "http://localhost:8428/api/v1/query?query=up"
curl "http://localhost:8428/api/v1/label/__name__/values"
```

## Демонстрация SLI/SLO/SLA

Масштабируйте backend до трёх экземпляров:

```bash
kubectl -n cinema scale deployment/backend --replicas=3
kubectl -n cinema get pods -w
```

Отправляйте запросы к каталогу через frontend и наблюдайте за изменениями на дашборде. Для демонстрации ошибок каталога остановите PostgreSQL, продолжая отправлять запросы:

```bash
kubectl -n cinema scale deployment/postgres --replicas=0
kubectl -n cinema scale deployment/postgres --replicas=1
```

После восстановления PostgreSQL дождитесь готовности pod-а и продолжите запросы. Синтетические проверки Blackbox Exporter помечены заголовком `X-Synthetic-Probe` и учитываются отдельно в `probe_success`, без влияния на пользовательские request-based SLI.

Для открытия интерфейса VictoriaMetrics используйте:

```bash
bash k8s/observability/vmui.sh
```
