# API Gateway - Developer Documentation

## 📋 Table of Contents

1. [Overview](#overview)
2. [Application Methods](#application-methods)
   - [ApiGatewayApplication.main()](#apigatewayapplicationmain)
   - [ApiGatewayApplicationTests.contextLoads()](#apigatewayapplicationtestscontextloads)
3. [Gateway Routes](#gateway-routes)
   - [Trains Service Route](#trains-service-route)
   - [Training Service Route](#training-service-route)
   - [Nutrition Service Route](#nutrition-service-route)
   - [Notification Service Route](#notification-service-route)
   - [Recommendation Service Route](#recommendation-service-route)
4. [Actuator Endpoints](#actuator-endpoints)
   - [Health Endpoint](#health-endpoint)
   - [Info Endpoint](#info-endpoint)
   - [Prometheus Metrics Endpoint](#prometheus-metrics-endpoint)
5. [Configuration Reference](#configuration-reference)
6. [Development Guide](#development-guide)

---

## Overview

**API Gateway** is a Spring Cloud Gateway application that serves as the single entry point for the Fitness Microservices Platform. It provides:

- **Dynamic Routing**: Forwards requests to appropriate microservices based on URL patterns
- **Load Balancing**: Distributes traffic across service instances using Eureka service discovery
- **Monitoring**: Exposes metrics for Prometheus and Grafana integration
- **Health Checks**: Provides endpoints for service health monitoring

**Technology Stack:**
- Java 21
- Spring Boot 3.4.2
- Spring Cloud Gateway 2024.0.0
- Eureka Client for service discovery
- Micrometer + Prometheus for metrics

---

## Application Methods

### ApiGatewayApplication.main()

**Location:** `src/main/java/com/example/api_gateway/ApiGatewayApplication.java`

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

#### Description
The main entry point for the Spring Boot application. This method bootstraps the entire API Gateway application and initializes all Spring components.

#### Annotations Explained

- **`@SpringBootApplication`**: A convenience annotation that combines three annotations:
  - `@Configuration`: Marks the class as a source of bean definitions
  - `@EnableAutoConfiguration`: Enables Spring Boot's auto-configuration mechanism
  - `@ComponentScan`: Tells Spring to look for other components, configurations, and services in the `com.example.api_gateway` package

- **`@EnableDiscoveryClient`**: Enables service discovery functionality, allowing this application to:
  - Register itself with Eureka Server on startup
  - Discover other microservices registered in Eureka
  - Use service names (like `lb://TRAINS-SERVICE`) for dynamic routing instead of hardcoded URLs

#### Parameters

- `String[] args`: Command-line arguments passed to the application. Can be used to override Spring Boot configuration properties.

#### Return Value

Returns a `ConfigurableApplicationContext` instance (though not captured in this code), which represents the running Spring application.

#### Usage

```bash
# Standard startup
java -jar api-gateway.jar

# With custom configuration
java -jar api-gateway.jar --server.port=9090

# With environment variable override
EUREKA_SERVER_URL=http://localhost:8761/eureka/ java -jar api-gateway.jar
```

#### What Happens During Startup

1. Spring Boot initializes the application context
2. Auto-configuration sets up Spring Cloud Gateway with routes from `application.yml`
3. Eureka client registers with the Eureka server
4. Gateway starts listening on port 8075
5. Actuator endpoints become available for monitoring

---

### ApiGatewayApplicationTests.contextLoads()

**Location:** `src/test/java/com/example/api_gateway/ApiGatewayApplicationTests.java`

```java
@SpringBootTest
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

#### Description
A smoke test that verifies the Spring Boot application context loads successfully without errors. This is a standard Spring Boot test that ensures all beans can be created and the application can start.

#### Annotations Explained

- **`@SpringBootTest`**: Tells Spring Boot to look for the main application class and create a full application context for testing. This loads the entire application configuration including:
  - All beans from component scanning
  - Auto-configuration
  - Properties from `application.yml`
  - Gateway routes
  - Eureka client configuration

- **`@Test`**: JUnit 5 annotation that marks this method as a test case

#### Purpose

This test validates that:
1. All dependencies are correctly configured
2. No circular dependencies exist
3. All beans can be instantiated
4. The application can start without runtime errors

#### When to Modify

Add additional tests here when you need to:
- Test custom configurations
- Verify route definitions
- Test custom beans or filters
- Integration test gateway behavior

#### Running the Test

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ApiGatewayApplicationTests

# Run with verbose output
mvn test -X
```

---

## Gateway Routes

All routes are configured in `src/main/resources/application.yml` using Spring Cloud Gateway's declarative configuration. The gateway uses **predicate-based routing** where each route has:

- **ID**: Unique identifier for the route
- **URI**: Target service URL (using `lb://` prefix for load-balanced service discovery)
- **Predicates**: Conditions that must match for the route to be used (e.g., path patterns)

### Trains Service Route

**Route ID:** `trains-service`

```yaml
- id: trains-service
  uri: lb://TRAINS-SERVICE
  predicates:
    - Path=/api/trains/**
```

#### Description
Routes all requests matching `/api/trains/**` to the TRAINS-SERVICE microservice.

#### How It Works

1. **Path Matching**: Any URL starting with `/api/trains/` triggers this route
2. **Service Discovery**: The `lb://` prefix tells Spring Cloud LoadBalancer to:
   - Query Eureka for instances of `TRAINS-SERVICE`
   - Select one instance using round-robin or another load balancing strategy
   - Forward the request to that instance

#### Example Requests

```bash
# Get all trains
GET http://localhost:8075/api/trains

# Get specific train
GET http://localhost:8075/api/trains/123

# Get train exercises
GET http://localhost:8075/api/trains/123/exercises
```

#### Path Transformation

The path is forwarded **as-is** to the target service. If TRAINS-SERVICE runs on `http://trains-service:8081`, the request becomes:
```
Client: GET http://localhost:8075/api/trains/123
Gateway: GET http://trains-service:8081/api/trains/123
```

#### Troubleshooting

- **404 Not Found**: TRAINS-SERVICE may not be running or not registered in Eureka
- **503 Service Unavailable**: No instances of TRAINS-SERVICE found in Eureka

---

### Training Service Route

**Route ID:** `training-service`

```yaml
- id: training-service
  uri: lb://TRAINING-SERVICE
  predicates:
    - Path=/api/training/**
```

#### Description
Routes all requests matching `/api/training/**` to the TRAINING-SERVICE microservice.

#### Purpose
Handles training-related operations such as workout sessions, training programs, exercise tracking, and user training data.

#### Example Requests

```bash
# Get training programs
GET http://localhost:8075/api/training/programs

# Create a training session
POST http://localhost:8075/api/training/sessions
Content-Type: application/json

{
  "userId": 1,
  "programId": 5,
  "date": "2026-04-11"
}

# Get user training history
GET http://localhost:8075/api/training/users/1/history
```

#### Use Cases

- Training program management
- Workout session tracking
- Exercise performance logging
- Training progress analytics

---

### Nutrition Service Route

**Route ID:** `nutrition-service`

```yaml
- id: nutrition-service
  uri: lb://NUTRITION-SERVICE
  predicates:
    - Path=/api/nutrition/**
```

#### Description
Routes all requests matching `/api/nutrition/**` to the NUTRITION-SERVICE microservice.

#### Purpose
Manages nutrition-related data including food items, meal plans, dietary tracking, and nutritional information.

#### Example Requests

```bash
# Get food database
GET http://localhost:8075/api/nutrition/foods

# Get meal plans
GET http://localhost:8075/api/nutrition/meal-plans

# Log a meal
POST http://localhost:8075/api/nutrition/meals
Content-Type: application/json

{
  "userId": 1,
  "foodId": "apple",
  "quantity": 150,
  "mealType": "breakfast"
}

# Get daily nutrition summary
GET http://localhost:8075/api/nutrition/users/1/daily-summary?date=2026-04-11
```

#### Use Cases

- Food and calorie tracking
- Meal planning
- Macronutrient monitoring
- Dietary recommendations

---

### Notification Service Route

**Route ID:** `notification-service`

```yaml
- id: notification-service
  uri: lb://NOTIFICATION-SERVICE
  predicates:
    - Path=/api/notifications/**
```

#### Description
Routes all requests matching `/api/notifications/**` to the NOTIFICATION-SERVICE microservice.

#### Purpose
Handles notification delivery, user preferences for notifications, and notification history.

#### Example Requests

```bash
# Get user notifications
GET http://localhost:8075/api/notifications/users/1

# Send a notification
POST http://localhost:8075/api/notifications/send
Content-Type: application/json

{
  "userId": 1,
  "message": "Your training session starts in 30 minutes!",
  "type": "reminder"
}

# Update notification preferences
PUT http://localhost:8075/api/notifications/users/1/preferences
Content-Type: application/json

{
  "emailEnabled": true,
  "pushEnabled": false,
  "reminderFrequency": "daily"
}

# Mark notification as read
PATCH http://localhost:8075/api/notifications/123/read
```

#### Use Cases

- Training reminders
- Achievement notifications
- System announcements
- Email/SMS/Push notification delivery

---

### Recommendation Service Route

**Route ID:** `recommendation-service`

```yaml
- id: recommendation-service
  uri: lb://RECOMMENDATION-SERVICE
  predicates:
    - Path=/api/recommendations/**
```

#### Description
Routes all requests matching `/api/recommendations/**` to the RECOMMENDATION-SERVICE microservice.

#### Purpose
Provides personalized recommendations for training programs, nutrition plans, and fitness goals based on user data.

#### Example Requests

```bash
# Get training recommendations for user
GET http://localhost:8075/api/recommendations/user/1/training

# Get nutrition recommendations
GET http://localhost:8075/api/recommendations/user/1/nutrition

# Generate new recommendations
POST http://localhost:8075/api/recommendations/user/1/generate
Content-Type: application/json

{
  "goal": "muscle_gain",
  "timeframe": "12_weeks"
}

# Get recommended workouts
GET http://localhost:8075/api/recommendations/user/1/workouts?level=intermediate
```

#### Use Cases

- Personalized training suggestions
- Diet plan recommendations
- Goal-based program selection
- Progress-based adaptations

---

## Actuator Endpoints

Spring Boot Actuator provides production-ready features for monitoring and managing the application. These endpoints are exposed on the same port (8075) under the `/actuator` path.

### Health Endpoint

**URL:** `http://localhost:8075/actuator/health`

**Method:** GET

**Authentication:** None (configured for `show-details: always`)

#### Description
Provides health information about the application and its dependencies. Returns the overall health status and detailed information about components.

#### Response Example

```json
{
  "status": "UP",
  "components": {
    "discoveryComposite": {
      "status": "UP",
      "details": {
        "services": {
          "TRAINS-SERVICE": ["http://trains-service-1:8081"],
          "TRAINING-SERVICE": ["http://training-service-1:8082"],
          "NUTRITION-SERVICE": ["http://nutrition-service-1:8083"],
          "NOTIFICATION-SERVICE": ["http://notification-service-1:8084"],
          "RECOMMENDATION-SERVICE": ["http://recommendation-service-1:8085"]
        }
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 536870912000,
        "free": 268435456000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

#### Status Values

- **UP**: Application is functioning correctly
- **DOWN**: Application is experiencing issues
- **OUT_OF_SERVICE**: Application is running but should not receive traffic
- **UNKNOWN**: Status cannot be determined

#### Health Indicators

The endpoint checks:
- **Discovery Client**: Can connect to Eureka and discover services
- **Disk Space**: Sufficient disk space available
- **Ping**: Basic application liveness check

#### Usage

```bash
# Quick health check
curl http://localhost:8075/actuator/health

# Check if application is ready for traffic
curl -s http://localhost:8075/actuator/health | jq -r '.status'
```

---

### Info Endpoint

**URL:** `http://localhost:8075/actuator/info`

**Method:** GET

#### Description
Displays arbitrary application information. Can be customized by adding info properties to `application.yml` or `application.properties`.

#### Response Example

```json
{
  "app": {
    "name": "API_Gateway",
    "version": "1.0.0",
    "description": "Spring Cloud Gateway for Fitness Microservices Platform"
  }
}
```

#### Customization

Add to `application.yml`:

```yaml
info:
  app:
    name: ${spring.application.name}
    version: @project.version@
    description: API Gateway for Fitness Platform
  build:
    java:
      version: ${java.version}
```

#### Usage

```bash
# Get application info
curl http://localhost:8075/actuator/info
```

---

### Prometheus Metrics Endpoint

**URL:** `http://localhost:8075/actuator/prometheus`

**Method:** GET

#### Description
Exposes application metrics in Prometheus format. These metrics are scraped by Prometheus and can be visualized in Grafana.

#### Metrics Provided

**HTTP Metrics:**
- `http_server_requests_seconds`: Request duration and count
- `http_server_requests_seconds_max`: Maximum request duration
- `http_server_requests_active_seconds`: Active request duration

**JVM Metrics:**
- `jvm_memory_used_bytes`: JVM memory usage
- `jvm_memory_max_bytes`: Maximum JVM memory
- `jvm_memory_committed_bytes`: Committed JVM memory
- `jvm_gc_pause_seconds`: Garbage collection pause time
- `jvm_threads_live_threads`: Live thread count
- `jvm_threads_daemon_threads`: Daemon thread count

**System Metrics:**
- `system_cpu_usage`: System CPU usage
- `process_cpu_usage`: Process CPU usage
- `system_load_average_1m`: System load average

**Gateway Metrics:**
- `spring_cloud_gateway_requests_seconds`: Gateway request duration
- `spring_cloud_gateway_requests_total`: Total gateway requests
- `spring_cloud_gateway_responses_total`: Total gateway responses by status code

#### Response Example

```
# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds{application="API_Gateway",exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/trains/{id}",quantile="0.5",} 0.045
http_server_requests_seconds_count{application="API_Gateway",exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/trains/{id}",} 150.0
http_server_requests_seconds_sum{application="API_Gateway",exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/trains/{id}",} 6.75
```

#### Usage

```bash
# Get metrics in Prometheus format
curl http://localhost:8075/actuator/prometheus

# Filter specific metrics
curl http://localhost:8075/actuator/prometheus | grep jvm_memory
```

#### Prometheus Configuration

Prometheus scrapes this endpoint based on configuration in `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8075']
```

---

## Configuration Reference

### Application Configuration

**File:** `src/main/resources/application.yml`

#### Server Configuration

```yaml
server:
  port: 8075
```

- **port: 8075**: The port on which the API Gateway listens for incoming requests
- Can be overridden via environment variable: `SERVER_PORT=9090`

#### Spring Application Configuration

```yaml
spring:
  application:
    name: API_Gateway
```

- **name: API_Gateway**: The application name used for service registration in Eureka
- This name appears in Eureka dashboard and service discovery queries

#### Gateway Routes Configuration

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
```

- **discovery.locator.enabled: true**: Enables automatic route discovery using service names from Eureka
- Allows using `lb://SERVICE-NAME` URIs for load-balanced routing

#### Management Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

- **endpoints.web.exposure.include**: Specifies which actuator endpoints are exposed via HTTP
- Only `health`, `info`, and `prometheus` are accessible; others are hidden for security

```yaml
management:
  endpoint:
    prometheus:
      enabled: true
    health:
      show-details: always
```

- **prometheus.enabled**: Enables the Prometheus metrics endpoint
- **health.show-details: always**: Always show detailed health information (useful for monitoring)

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
```

- **metrics.tags.application**: Adds an `application` tag to all metrics for identification in Prometheus/Grafana

#### Eureka Configuration

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://eureka-server:8761/eureka/}
  instance:
    prefer-ip-address: true
```

- **client.service-url.defaultZone**: URL of the Eureka server for service registration
  - Uses environment variable `EUREKA_SERVER_URL` if set
  - Falls back to `http://eureka-server:8761/eureka/` by default
- **instance.prefer-ip-address: true**: Register with IP address instead of hostname for better Docker compatibility

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8075` | Port on which the gateway runs |
| `EUREKA_SERVER_URL` | `http://eureka-server:8761/eureka/` | Eureka server URL |
| `JAVA_OPTS` | (none) | JVM options (e.g., `-Xmx512m -Xms256m`) |

### Docker Configuration

**File:** `docker-compose.yml`

```yaml
version: '3.8'
services:
  eureka-server:
    image: eureka-server:latest
    ports:
      - "8761:8761"
  
  api-gateway:
    build: .
    ports:
      - "8075:8075"
    environment:
      - EUREKA_SERVER_URL=http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server
```

**Key Points:**
- Gateway depends on Eureka server being available
- Environment variables are passed to configure Eureka client

---

## Development Guide

### Project Structure

```
API_Gateway/
├── src/
│   ├── main/
│   │   ├── java/com/example/api_gateway/
│   │   │   └── ApiGatewayApplication.java      # Main application entry point
│   │   └── resources/
│   │       └── application.yml                  # Gateway routes and configuration
│   └── test/
│       └── java/com/example/api_gateway/
│           └── ApiGatewayApplicationTests.java  # Smoke test
├── docker-compose.yml                           # Docker orchestration
├── Dockerfile                                   # Container image definition
├── pom.xml                                      # Maven dependencies
├── prometheus.yml                               # Prometheus scrape config
└── monitoring/                                  # Monitoring stack
    ├── docker-compose.monitoring.yml
    ├── prometheus/
    └── grafana/
```

### Adding a New Route

To add routing for a new microservice:

1. **Edit `application.yml`:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ... existing routes ...
        
        - id: new-service-route
          uri: lb://NEW-SERVICE
          predicates:
            - Path=/api/new-resource/**
```

2. **Ensure the service is registered in Eureka** with the name `NEW-SERVICE`

3. **Restart the gateway** to pick up the new route

### Adding Custom Filters

Spring Cloud Gateway supports filters for request/response manipulation:

**Example: Add logging filter**

```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request: {} {}", exchange.getRequest().getMethod(), 
                                   exchange.getRequest().getURI());
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -1; // Order of execution
    }
}
```

### Adding Route-Specific Filters

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: trains-service
          uri: lb://TRAINS-SERVICE
          predicates:
            - Path=/api/trains/**
          filters:
            - AddRequestHeader=X-Gateway-Source, API-Gateway
            - AddResponseHeader=X-Response-Time, ${response.time}
            - StripPrefix=1
```

**Common Filters:**
- **AddRequestHeader**: Adds header to request
- **AddResponseHeader**: Adds header to response
- **StripPrefix**: Removes path segments before forwarding
- **RewritePath**: Rewrites the request path using regex
- **RequestRateLimiter**: Rate limiting

### Testing the Gateway

#### Manual Testing

```bash
# Test route to trains service
curl http://localhost:8075/api/trains

# Test health endpoint
curl http://localhost:8075/actuator/health

# Test metrics endpoint
curl http://localhost:8075/actuator/prometheus

# Test with verbose output
curl -v http://localhost:8075/api/trains
```

#### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 10 http://localhost:8075/api/trains/

# Using curl in a loop
for i in {1..100}; do
  curl -s http://localhost:8075/api/trains &
done
wait
```

### Debugging

#### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.cloud.loadbalancer: DEBUG
    com.netflix.discovery: DEBUG
```

#### Common Issues

**Problem:** Route not working, getting 404

**Solutions:**
1. Check if target service is running and registered in Eureka
2. Verify route configuration in `application.yml`
3. Check Eureka dashboard at `http://localhost:8761`
4. Enable debug logging (see above)

**Problem:** 503 Service Unavailable

**Solutions:**
1. Target service is not registered in Eureka
2. Service name in route doesn't match Eureka registration
3. Check service logs for startup issues

**Problem:** Cannot connect to Eureka

**Solutions:**
1. Verify Eureka server is running
2. Check `EUREKA_SERVER_URL` environment variable
3. Ensure network connectivity between containers

### Monitoring with Grafana

1. **Start monitoring stack:**

```bash
cd monitoring
docker-compose -f docker-compose.monitoring.yml up -d
```

2. **Access Grafana:**
   - URL: `http://localhost:3000`
   - Username: `admin`
   - Password: `admin`

3. **Import dashboard:**
   - Go to Dashboards → Import
   - Use `monitoring/grafana/dashboards/gateway-dashboard.json`

4. **Key metrics to monitor:**
   - Request rate (requests per second)
   - Response time (p50, p95, p99)
   - Error rate (5xx responses)
   - JVM memory usage
   - Gateway route statistics

### Deployment

#### Docker Deployment

```bash
# Build image
docker build -t api-gateway:latest .

# Run container
docker run -d \
  --name api-gateway \
  -p 8075:8075 \
  -e EUREKA_SERVER_URL=http://eureka-server:8761/eureka/ \
  api-gateway:latest
```

#### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: api-gateway:latest
        ports:
        - containerPort: 8075
        env:
        - name: EUREKA_SERVER_URL
          value: "http://eureka-server:8761/eureka/"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
spec:
  selector:
    app: api-gateway
  ports:
  - port: 8075
    targetPort: 8075
  type: LoadBalancer
```

---

## Quick Reference

### Ports

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8075 | Main gateway port |
| Eureka Server | 8761 | Service registry |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics visualization |

### Important URLs

| Service | URL |
|---------|-----|
| Gateway | http://localhost:8075 |
| Eureka Dashboard | http://localhost:8761 |
| Health Check | http://localhost:8075/actuator/health |
| Metrics | http://localhost:8075/actuator/prometheus |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

### Common Commands

```bash
# Start application stack
docker-compose up --build

# Start monitoring
cd monitoring && docker-compose -f docker-compose.monitoring.yml up -d

# Build project
mvn clean package

# Run tests
mvn test

# Run with skip tests
mvn clean package -DskipTests

# View logs
docker-compose logs -f api-gateway

# Restart gateway
docker-compose restart api-gateway
```

---

## Support & Troubleshooting

### Logs Location

```bash
# Docker logs
docker-compose logs api-gateway

# Follow logs in real-time
docker-compose logs -f api-gateway

# Last 100 lines
docker-compose logs --tail=100 api-gateway
```

### Useful Queries for Troubleshooting

```bash
# Check if all services are registered in Eureka
curl http://localhost:8761/eureka/apps

# Get gateway routes
curl http://localhost:8075/actuator/gateway/routes

# Check service health
curl http://localhost:8075/actuator/health | jq .components

# View gateway metrics
curl http://localhost:8075/actuator/prometheus | grep gateway
```

---

## Contributing

When modifying the API Gateway:

1. **Test routes locally** before committing
2. **Update this documentation** when adding new routes or features
3. **Run tests** with `mvn test`
4. **Check metrics** in Grafana to ensure performance is acceptable
5. **Update CI/CD** pipeline if adding new dependencies

---

*Last Updated: April 11, 2026*
*Version: 1.0.0*
