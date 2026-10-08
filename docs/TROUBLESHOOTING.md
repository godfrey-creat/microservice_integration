# Troubleshooting Guide

How to find and fix problems with the Country Info Service on Kubernetes.

All commands use the `country-info` namespace. To avoid typing `-n country-info` every time:

```bash
kubectl config set-context --current --namespace=country-info
```

## 1. First checks

Start here whenever something is wrong:

```bash
kubectl get pods                                        # status and restarts
kubectl get events --sort-by=.lastTimestamp | tail -20  # recent errors
kubectl logs -l app=country-info-service --tail=50      # application logs
```

Then find your problem below.

## 2. Pod problems

### `ImagePullBackOff` or `ErrImagePull`

**Cause:** The cluster cannot find the image.

**Fix:** Load the image into minikube, then restart the deployment:

```bash
minikube image load country-info-service:1.0.0
kubectl rollout restart deployment/country-info-service
```

### Pod stuck at `Init:0/1`

**Cause:** The app is waiting for MySQL to start.

**Fix:** Check MySQL:

```bash
kubectl get pods -l app=mysql
kubectl logs mysql-0
```

The first start of MySQL takes about a minute. If `mysql-0` is not `Running`, see [section 4](#4-database-problems).

### `CrashLoopBackOff`

**Cause:** The app starts and then crashes.

**Fix:** Read the logs from the crashed run:

```bash
kubectl logs <pod-name> --previous
```

| Error in the logs | Cause | Fix |
|---|---|---|
| `Communications link failure` | App cannot reach MySQL | See [section 4](#4-database-problems) |
| `Access denied for user` | Wrong database password | Check `k8s/02-secret.yaml` matches what MySQL was created with |
| `FlywayException` | Database migration failed | Check the migration files in `src/main/resources/db/migration` |
| `OOMKilled` (in `kubectl describe pod`) | Not enough memory | Increase `limits.memory` in `k8s/20-app-deployment.yaml` |

### Pod is `Running` but shows `0/1` ready

**Cause:** The readiness check is failing, usually because the database is not reachable.

**Fix:** Check the readiness endpoint:

```bash
kubectl port-forward pod/<pod-name> 8080:8080
curl localhost:8080/actuator/health/readiness
```

If it shows `"db":{"status":"DOWN"}`, see [section 4](#4-database-problems).

### Pod stuck in `Pending`

**Cause:** The cluster does not have enough memory or CPU.

**Fix:** Check the reason, then give minikube more resources:

```bash
kubectl describe pod <pod-name> | tail -10
minikube stop
minikube start --driver=docker --cpus=2 --memory=6g
```

## 3. API errors

Every response includes an `X-Correlation-Id` header. Use it to find all log lines for that request:

```bash
kubectl logs -l app=country-info-service --tail=500 | grep <correlation-id>
```

| Status | Meaning | What to do |
|---|---|---|
| `400` | Bad input (empty name, invalid id, bad JSON) | Check the request; the response lists the invalid fields |
| `404` on POST | The SOAP service does not know that country name | Check the spelling |
| `404` on GET, PUT, DELETE | No country with that id | List countries with `GET /api/v1/countries` |
| `409` | Two updates to the same country at once | Retry the request |
| `502` | The SOAP service returned an error | Check the logs for `soap_http_error` |
| `503` | The SOAP service is down and the country is not stored yet | Wait and retry (see [section 5](#5-soap-service-problems)) |

## 4. Database problems

### `Communications link failure`

**Cause:** MySQL is not running or not reachable.

**On Kubernetes:**

```bash
kubectl get pods -l app=mysql
kubectl logs mysql-0
kubectl get endpoints mysql      # must list an IP address
```

If `mysql-0` is not running, restart it:

```bash
kubectl delete pod mysql-0       # Kubernetes recreates it; data is kept
```

**On your laptop (local development):** the MySQL Docker container has usually stopped, for example after a restart.

```bash
docker start country-mysql
docker update --restart unless-stopped country-mysql   # restart it automatically in future
```

### Check the data directly

```bash
kubectl exec -it mysql-0 -- mysql -ucountry_user -p countrydb
```

Then:

```sql
SHOW TABLES;
SELECT id, iso_code, name FROM country_info;
```

## 5. SOAP service problems

### Check if the circuit breaker is open

```bash
kubectl port-forward deploy/country-info-service 8080:8080
curl localhost:8080/actuator/circuitbreakers
```

| State | Meaning |
|---|---|
| `CLOSED` | Normal |
| `OPEN` | The SOAP service failed too often; calls are blocked for 30 seconds |
| `HALF_OPEN` | Testing whether the SOAP service has recovered |

While it is `OPEN`, countries that are already stored are still returned (`X-Data-Source: DATABASE_FALLBACK`). It closes by itself once the SOAP service recovers.

### Check if the pod can reach the SOAP service

```bash
kubectl exec deploy/country-info-service -- wget -qO- -T 5 \
  'http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL' | head -5
```

If this fails, the cluster has no internet access. Check your network or firewall.

### Multi-word country names return `404`

**Cause:** The SOAP service matches names case-sensitively (`South Africa` works, `South africa` does not).

**Fix:** None needed. The app tries sentence case first (as required), then title case automatically. If a name still fails, check its exact spelling in SoapUI.

## 6. Access problems

### `port-forward` or `curl` cannot connect

```bash
kubectl get endpoints country-info-service   # empty means no pod is ready
```

If it is empty, fix the pods first ([section 2](#2-pod-problems)).

### Ingress returns `404` or does not respond

```bash
minikube addons enable ingress
grep country-info.local /etc/hosts           # must show the minikube IP
echo "$(minikube ip) country-info.local" | sudo tee -a /etc/hosts
```

## 7. Setup problems

| Problem | Fix |
|---|---|
| `minikube: command not found` | `curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64 && sudo install minikube-linux-amd64 /usr/local/bin/minikube` |
| `permission denied` when running `docker` | `sudo usermod -aG docker $USER && newgrp docker` |
| `Port 8080 was already in use` | Stop the other process: `ss -ltnp \| grep 8080`, then `kill <pid>` |
| HPA shows `<unknown>` targets | `minikube addons enable metrics-server`, then wait 1 minute |

## 8. Roll back a bad release

```bash
kubectl rollout history deployment/country-info-service
kubectl rollout undo deployment/country-info-service
```

## 9. Start fresh

If nothing else works, remove everything and deploy again:

```bash
./scripts/teardown.sh
./scripts/deploy.sh minikube
```