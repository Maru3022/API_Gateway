# API Gateway

Spring Cloud Gateway for the Fitness Microservices Platform.

## What Is Included
- Dynamic routing to backend microservices through Eureka service discovery.
- Actuator endpoints for health and Prometheus metrics.
- Monitoring stack (Prometheus + Grafana) via Docker Compose.
- Kubernetes manifests for production-style deployment.
- Advanced GitHub Actions CI/CD pipeline with security gates and image publishing.

## Architecture
Client -> API Gateway (:8075) -> discovered services via Eureka (:8761)

Routes configured in [src/main/resources/application.yml](src/main/resources/application.yml):
- `/api/trains/**` -> `TRAINS-SERVICE`
- `/api/training/**` -> `TRAINING-SERVICE`
- `/api/nutrition/**` -> `NUTRITION-SERVICE`
- `/api/notifications/**` -> `NOTIFICATION-SERVICE`
- `/api/recommendations/**` -> `RECOMMENDATION-SERVICE`

## Requirements
- Java 21
- Maven 3.9+
- Docker (for monitoring and image build)
- Kubernetes cluster 1.27+ (for k8s deployment)
- Eureka Server reachable from Gateway and microservices

## Local Run (Without Kubernetes)
1. Start Eureka Server (for local use typically `http://localhost:8761/eureka/`).
2. Start backend microservices and ensure they register in Eureka with expected names.
3. Start API Gateway:

```powershell
cd C:\Project\API_Gateway
$env:EUREKA_SERVER_URL="http://localhost:8761/eureka/"
.\mvnw.cmd clean package -DskipTests
java -jar target\API_Gateway-0.0.1-SNAPSHOT.jar
```

Health and metrics:
- `http://localhost:8075/actuator/health`
- `http://localhost:8075/actuator/prometheus`

## Docker Image
Repository contains a production-oriented `Dockerfile`.

```powershell
cd C:\Project\API_Gateway
.\mvnw.cmd clean package -DskipTests
docker build -t ghcr.io/<your-user-or-org>/api-gateway:local .
```

## Kubernetes Deployment
Kubernetes manifests are in `k8s/`:
- `namespace.yaml`
- `configmap.yaml`
- `deployment.yaml`
- `service.yaml`
- `hpa.yaml`
- `ingress.yaml`
- `kustomization.yaml`

### Deploy
1. Replace image in `k8s/deployment.yaml` and `k8s/kustomization.yaml`:
   - `ghcr.io/<your-user-or-org>/api-gateway:<tag>`
2. Apply manifests:

```bash
kubectl apply -k k8s
```

3. Verify rollout:

```bash
kubectl -n fitness-platform get pods,svc,hpa,ingress
kubectl -n fitness-platform rollout status deploy/api-gateway
```

### Notes
- Readiness/Liveness probes use `/actuator/health`.
- HPA scales from 2 to 8 replicas by CPU and memory utilization.
- `EUREKA_SERVER_URL` is managed via `ConfigMap`.

## Monitoring (Docker Compose)
```powershell
cd C:\Project\API_Gateway
docker network create training-network
cd monitoring
docker compose -f docker-compose.monitoring.yml up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

Important: `monitoring/prometheus/prometheus.yml` scrapes `api-gateway:8075`. If gateway runs outside that network, change target accordingly.

## Environment Variables
- `EUREKA_SERVER_URL` (default: `http://eureka-server:8761/eureka/`)
- `SERVER_PORT` (default: `8075`)
- `JAVA_OPTS` (optional JVM tuning)

## CI/CD (GitHub Actions)
Workflow: `.github/workflows/main.yml`

Pipeline stages:
1. Kubernetes manifests render + schema validation (`kustomize` + `kubeconform`).
2. Maven build and tests (`mvn clean verify`).
3. Filesystem vulnerability scan (Trivy SARIF).
4. Docker buildx image build (and push on non-PR events to GHCR).
5. Container image vulnerability scan (Trivy SARIF).
6. SBOM generation (CycloneDX) for published image.

## Quick Smoke Test
```bash
curl http://localhost:8075/actuator/health
curl http://localhost:8075/api/trains
curl http://localhost:8075/api/nutrition/foods
```

## Troubleshooting
- `503 from Gateway`: service not registered in Eureka or name mismatch.
- `Cannot connect to Eureka`: wrong `EUREKA_SERVER_URL` or network/DNS issue.
- `Prometheus has target DOWN`: wrong scrape target or gateway unreachable.

## Security Baseline Implemented
- Non-root container runtime.
- Read-only root filesystem in Kubernetes pod.
- Dropped Linux capabilities.
- Trivy security scans in CI for source and image.
- SBOM generation in CI.
