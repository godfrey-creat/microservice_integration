# Deployment Guide

How to deploy the Country Info Service and its MySQL database to Kubernetes (minikube).

Repository: https://github.com/godfrey-creat/microservice_integration

## 1. Prerequisites

Install these first:

- Java 17
- Docker (`docker ps` must work without `sudo`)
- kubectl
- minikube

Check them:

```bash
java -version
docker --version
kubectl version --client
minikube version
```

## 2. Get the code and run the tests

```bash
git clone https://github.com/godfrey-creat/microservice_integration.git
cd microservice_integration
./mvnw clean test
```

All tests should pass (`BUILD SUCCESS`).

## 3. Start minikube

```bash
minikube start --driver=docker --cpus=2 --memory=4g
minikube addons enable metrics-server
minikube addons enable ingress
kubectl get nodes
```

The node should show `Ready`.

## 4. Deploy

Run the deploy script:

```bash
./scripts/deploy.sh minikube
```

This builds the Docker image, loads it into minikube, applies everything in `k8s/` and waits until it is ready.
The first run takes 2–4 minutes.

When it finishes, all pods should be `Running` and `1/1` ready:

```bash
kubectl -n country-info get pods
```

```
NAME                                    READY   STATUS    RESTARTS
country-info-service-xxxxxxxxxx-aaaaa   1/1     Running   0
country-info-service-xxxxxxxxxx-bbbbb   1/1     Running   0
mysql-0                                 1/1     Running   0
```

## 5. Test the deployment

Open a tunnel to the service (leave this running):

```bash
kubectl -n country-info port-forward svc/country-info-service 8080:80
```

In a second terminal:

```bash
# Register a country
curl -X POST localhost:8080/api/v1/countries -H 'Content-Type: application/json' -d '{"name":"kenya"}'

# List all countries
curl localhost:8080/api/v1/countries

# Get one country
curl localhost:8080/api/v1/countries/1

# Update a country
curl -X PUT localhost:8080/api/v1/countries/1 -H 'Content-Type: application/json' \
  -d '{"name":"Kenya","capitalCity":"Nairobi","phoneCode":"254","continentCode":"AF","currencyIsoCode":"KES"}'

# Delete a country
curl -X DELETE localhost:8080/api/v1/countries/1

# Health check
curl localhost:8080/actuator/health/readiness
```

## 6. What is deployed

All files are in the `k8s/` folder and are applied together with `kubectl apply -k k8s/`.

| File | What it does |
|---|---|
| `00-namespace.yaml` | Creates the `country-info` namespace |
| `01-configmap.yaml` | App settings (database URL, SOAP URL, timeouts) |
| `02-secret.yaml` | Database username and passwords |
| `10-mysql.yaml` | MySQL database with persistent storage |
| `20-app-deployment.yaml` | The application (2 pods, health checks, resource limits) |
| `21-app-service.yaml` | Load balances traffic across the app pods |
| `22-hpa.yaml` | Auto-scales from 2 to 10 pods based on CPU |
| `23-pdb.yaml` | Keeps at least 1 pod running during maintenance |
| `24-ingress.yaml` | External access at `country-info.local` |
| `30-networkpolicy.yaml` | Only the app can connect to MySQL |

## 7. Common tasks

Change a setting: edit `k8s/01-configmap.yaml`, then:

```bash
kubectl apply -k k8s/
kubectl -n country-info rollout restart deployment/country-info-service
```

Deploy a new version:

```bash
TAG=1.0.1 ./scripts/deploy.sh minikube
kubectl -n country-info set image deployment/country-info-service app=country-info-service:1.0.1
```

Roll back to the previous version:

```bash
kubectl -n country-info rollout undo deployment/country-info-service
```

View logs:

```bash
kubectl -n country-info logs -l app=country-info-service --tail=50
```

## 8. Remove everything

```bash
./scripts/teardown.sh
minikube stop
```

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).