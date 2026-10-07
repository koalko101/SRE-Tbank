# Kubernetes

Манифесты рассчитаны на локальный кластер `minikube` или Docker Desktop Kubernetes.

```bash
docker build -t cinema-backend:latest .
docker build -t cinema-frontend:latest ./frontend
kubectl apply -f k8s/
kubectl -n cinema get pods
```

Frontend будет доступен через NodePort `30080`. Для minikube: `minikube service frontend -n cinema`. Backend масштабируется до двух реплик, PostgreSQL сохраняет данные через PVC, а секреты и конфигурация передаются отдельно через Secret и ConfigMap.
