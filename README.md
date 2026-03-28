# 🌐 API Gateway

A Spring Cloud Gateway service acting as the single entry point for the **Fitness Microservices Platform**. Handles dynamic routing, load balancing via Eureka, and exposes metrics for Prometheus/Grafana monitoring.

---

## 📐 Architecture

```
Client Request
      │
      ▼
 API Gateway :8075
      │
      ├─ /api/trains/**          ──▶  TRAINS-SERVICE
      ├─ /api/training/**        ──▶  TRAINING-SERVICE
      ├─ /api/nutrition/**       ──▶  NUTRITION-SERVICE
      ├─ /api/notifications/**   ──▶  NOTIFICATION-SERVICE
      └─ /api/recommendations/** ──▶  RECOMMENDATION-SERVICE

         ↕ service discovery
      Eureka Server :8761
```

---

## 📁 Project Structure

```
API_Gateway/
├── .github/
│   └── workflows/
│       └── main.yml                   # CI/CD pipeline
├── .mvn/
├── monitoring/
│   ├── grafana/
│   │   ├── dashboards/
│   │   │   └── gateway-dashboard.json
│   │   └── provisioning/
│   │       ├── dashboards/
│   │       │   └── dashboards.yml
│   │       └── datasources/
│   │           └── datasource.yml
│   ├── prometheus/
│   │   └── prometheus.yml
│   └── docker-compose.monitoring.yml  # Monitoring stack
├── src/
│   ├── main/
│   │   ├── java/com/example/api_gateway/
│   │   │   └── ApiGatewayApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── docker-compose.yml                 # App stack (gateway + eureka)
├── Dockerfile                         # Multi-stage build
└── pom.xml
```

---

## 🚀 Getting Started

### Option 1 — Docker Compose (recommended)

```bash
# Start Eureka + API Gateway
docker-compose up --build
```

| Service      | URL                         |
|--------------|-----------------------------|
| API Gateway  | http://localhost:8075        |
| Eureka UI    | http://localhost:8761        |

### Option 2 — Run locally (Maven)

> ⚠️ Requires Eureka Server running on `http://localhost:8761`

```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

---

## 📊 Monitoring (Prometheus + Grafana)

Run the monitoring stack separately **after** the main app is up:

```bash
# From the monitoring/ directory
cd monitoring
docker-compose -f docker-compose.monitoring.yml up -d
```

| Service    | URL                    | Credentials  |
|------------|------------------------|--------------|
| Prometheus | http://localhost:9090  | —            |
| Grafana    | http://localhost:3000  | admin / admin |

Metrics endpoint exposed at: `http://localhost:8075/actuator/prometheus`

---

## 🌍 Routing Table

| Route ID                  | Path Prefix              | Target Service          |
|---------------------------|--------------------------|-------------------------|
| `trains-service`          | `/api/trains/**`         | `TRAINS-SERVICE`        |
| `training-service`        | `/api/training/**`       | `TRAINING-SERVICE`      |
| `nutrition-service`       | `/api/nutrition/**`      | `NUTRITION-SERVICE`     |
| `notification-service`    | `/api/notifications/**`  | `NOTIFICATION-SERVICE`  |
| `recommendation-service`  | `/api/recommendations/**`| `RECOMMENDATION-SERVICE`|

### Example requests

```bash
curl http://localhost:8075/api/trains
curl http://localhost:8075/api/nutrition/foods
curl http://localhost:8075/api/recommendations/user/1
```

---

## ⚙️ Configuration

### `application.yml`

```yaml
server:
  port: 8075

spring:
  application:
    name: API_Gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
      routes:
        - id: trains-service
          uri: lb://TRAINS-SERVICE
          predicates:
            - Path=/api/trains/**
        # ... other routes

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://eureka-server:8761/eureka/}
  instance:
    prefer-ip-address: true
```

### Environment Variables

| Variable           | Default                                  | Description              |
|--------------------|------------------------------------------|--------------------------|
| `EUREKA_SERVER_URL`| `http://eureka-server:8761/eureka/`      | URL of the Eureka registry |

---

## 🐳 Docker

### Dockerfile (multi-stage)

```
Stage 1 — Build   (maven:3.9.6-eclipse-temurin-21)
  └── mvn clean package -DskipTests

Stage 2 — Runtime (eclipse-temurin:21-jre)
  └── java -jar app.jar
```

```bash
# Build image manually
docker build -t api-gateway .

# Run manually
docker run -p 8075:8075 \
  -e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ \
  api-gateway
```

---

## 🔄 CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/main.yml`) runs on every push/PR to `main`:

```
┌─────────────────────────────────────┐
│  Job 1: build                       │
│  • Checkout code                    │
│  • Set up JDK 21 (Temurin)         │
│  • mvn clean package -DskipTests    │
│  • Upload JAR as artifact           │
└──────────────┬──────────────────────┘
               │ needs: build
┌──────────────▼──────────────────────┐
│  Job 2: build-docker                │
│  • Checkout code                    │
│  • Download JAR artifact            │
│  • Set up Docker Buildx             │
│  • Build Docker image (push: false) │
└─────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Technology                         | Purpose                     |
|------------------------------------|-----------------------------|
| Java 21                            | Core language               |
| Spring Boot 3.4.2                  | Application framework       |
| Spring Cloud Gateway               | Routing & reverse proxy     |
| Spring Cloud Netflix Eureka Client | Service discovery           |
| Spring Boot Actuator               | Health & metrics endpoints  |
| Micrometer + Prometheus            | Metrics export              |
| Maven                              | Build tool                  |
| Docker (multi-stage)               | Containerization            |
| Prometheus + Grafana               | Monitoring & dashboards     |
| GitHub Actions                     | CI/CD                       |

---

## 🔗 Related Services

This gateway is part of the **Fitness Microservices Platform**. See the root [`README.md`](../README.md) for the full system architecture.