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
│   └── prometheus/
│       └── prometheus.yml
├── src/
│   ├── main/
│   │   ├── java/com/example/api_gateway/
│   │   │   └── ApiGatewayApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
└── pom.xml
```

---

## 🚀 Getting Started

### Run locally (Maven)

> ⚠️ Requires Eureka Server running on `http://localhost:8761`

```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

---

## 📊 Monitoring (Prometheus + Grafana)

Configure Prometheus and Grafana separately for monitoring.

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

## 🔄 CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/main.yml`) runs on every push/PR to `main`:

```
┌─────────────────────────────────────┐
│  Job: build-and-test                │
│  • Checkout code                    │
│  • Set up JDK 21 (Temurin)         │
│  • mvn clean verify                 │
│  • Trivy vulnerability scanning    │
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
| Prometheus + Grafana               | Monitoring & dashboards     |
| GitHub Actions                     | CI/CD                       |

---

## 🔗 Related Services

This gateway is part of the **Fitness Microservices Platform**. See the root [`README.md`](../README.md) for the full system architecture.