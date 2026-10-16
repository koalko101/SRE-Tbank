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
kubectl apply -f observability/
kubectl -n logging get pods,services,pvc
minikube service frontend -n cinema
```

Для открытия ```vmui``` нужно сделать port-forward:
```zsh
./observability/vmui.sh
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
