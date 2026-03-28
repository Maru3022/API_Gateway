# 🌐 API Gateway

A Spring Cloud Gateway service acting as the single entry point for the Fitness Microservices Platform. Handles dynamic routing, load balancing, and service discovery via Eureka.

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

## 🚀 Getting Started

### Run via Docker (recommended)

```bash
# Build the image
docker build -t api-gateway .

# Run the container
docker run -p 8075:8075 \
  -e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ \
  api-gateway
```

### Run locally (Maven)

```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

> ⚠️ Make sure Eureka Server is running on `http://localhost:8761` before starting the gateway.

---

## 🌍 Routing Table

All requests go through `http://localhost:8075` and are forwarded to the appropriate microservice via Eureka load balancing (`lb://`).

| Route ID | Path Prefix | Target Service |
|---|---|---|
| `trains-service` | `/api/trains/**` | `TRAINS-SERVICE` |
| `training-service` | `/api/training/**` | `TRAINING-SERVICE` |
| `nutrition-service` | `/api/nutrition/**` | `NUTRITION-SERVICE` |
| `notification-service` | `/api/notifications/**` | `NOTIFICATION-SERVICE` |
| `recommendation-service` | `/api/recommendations/**` | `RECOMMENDATION-SERVICE` |

### Example requests

```bash
# Trains
curl http://localhost:8075/api/trains

# Nutrition
curl http://localhost:8075/api/nutrition/foods

# Recommendations
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
          enabled: true   # Auto-discover services from Eureka
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

| Variable | Default | Description |
|---|---|---|
| `EUREKA_SERVER_URL` | `http://eureka-server:8761/eureka/` | URL of the Eureka registry |

---

## 🐳 Docker Build

Multi-stage Dockerfile for minimal image size:

```
Stage 1 — Build
  maven:3.9.6-eclipse-temurin-21
  └── mvn clean package -DskipTests

Stage 2 — Runtime
  eclipse-temurin:21-jre
  └── java -jar app.jar
```

```bash
# Build
docker build -t api-gateway .

# Inspect image
docker image inspect api-gateway
```

---

## 📁 Project Structure

```
API_Gateway/
├── Dockerfile
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/api_gateway/
        │   └── ApiGatewayApplication.java
        └── resources/
            └── application.yml
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core language |
| Spring Boot | Application framework |
| Spring Cloud Gateway | Routing & reverse proxy |
| Spring Cloud Netflix Eureka Client | Service discovery |
| Maven | Build tool |
| Docker (multi-stage) | Containerization |

---

## 🔗 Related Services

This gateway is part of the **Fitness Microservices Platform**. See the root [`README.md`](../README.md) for the full system architecture and Docker Compose setup.
