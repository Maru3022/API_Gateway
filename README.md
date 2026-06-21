# 🌐 API Gateway

Единая точка входа фитнес-платформы [FitFlow](https://github.com/Maru3022/project-hub). Реактивный Spring Cloud Gateway с JWT-аутентификацией, rate limiting на Redis, circuit breaker на каждый downstream-сервис и сквозной трассировкой запросов.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](.)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud%20Gateway-Reactive-6DB33F?logo=spring)](.)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-ff6b35)](.)
[![Redis](https://img.shields.io/badge/Redis-Rate%20Limiting-DC382D?logo=redis)](.)
[![JWT](https://img.shields.io/badge/Auth-JWT%20(HS256)-000000?logo=jsonwebtokens)](.)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-HPA%20%2F%20Ingress-326CE5?logo=kubernetes)](.)

---

## Что делает сервис

API Gateway — не «прокси для галочки», а полноценный edge-слой системы, на котором решены задачи, которые иначе пришлось бы дублировать в каждом из 6 бизнес-сервисов:

- **Маршрутизация** запросов к сервисам по имени из Eureka (`lb://service-name`), без хардкода адресов;
- **JWT-аутентификация на границе системы** — токен проверяется один раз на Gateway, а вниз по цепочке сервисы получают уже доверенный `X-User-Id`;
- **Rate limiting** — token bucket на Redis, отдельный лимит на клиента;
- **Circuit breaker на каждый маршрут** — отказ одного сервиса не валит всю систему, а отдаёт контролируемый fallback;
- **Сквозная трассировка** — `X-Correlation-Id` генерируется (или подхватывается) на входе и прокидывается через все логи и downstream-запросы;
- **Унифицированная обработка ошибок** — единый JSON-формат ошибки вместо «голого» стектрейса реактивного стека.

## Маршрутизация

| Маршрут | Сервис (Eureka) | Path Predicate |
|---|---|---|
| `/api/users/**` | `trains-service` | `StripPrefix=1` |
| `/api/cabinets/**` | `training-service` | `StripPrefix=1` |
| `/api/nutrition/**` | `training-nutrition` | `StripPrefix=1` |
| `/api/notifications/**` | `training-notification` | `StripPrefix=1` |
| `/api/recommendations/**` | `recommendation-service` | `StripPrefix=1` |
| `/api/saga/**` | `saga-orchestrator` | `StripPrefix=1` |

Каждый маршрут оборачивается одним и тем же набором фильтров — **rate limiter + circuit breaker** — конфигурация декларативная (`application.yml`), без копипасты Java-кода на каждый сервис.

## Цепочка фильтров запроса

```
Клиент
  │
  ▼
GlobalCorrelationIdFilter      — генерирует/подхватывает X-Correlation-Id (ORDER: HIGHEST_PRECEDENCE)
  │
  ▼
GlobalLoggingFilter            — структурированный лог: method, path, correlationId, status, durationMs
  │
  ▼
JwtAuthenticationFilter        — если есть Bearer-токен: валидирует подпись/срок, кладёт username в X-User-Id;
  │                               невалидный токен → 401 без похода в downstream
  ▼
RequestRateLimiter (Redis)     — token bucket по ключу клиента, 429 при превышении лимита
  │
  ▼
CircuitBreaker (Resilience4j)  — при открытой цепи или таймауте → forward:/fallback
  │
  ▼
lb://<service-name>            — балансировка нагрузки между инстансами через Eureka
```

При любой необработанной ошибке в реактивном пайплайне `GlobalErrorWebExceptionHandler` (`@Order(-2)`) гарантирует, что клиент получит структурированный JSON (`timestamp`, `path`, `status`, `error`, `requestId`), а не сырой стектрейс.

## Безопасность

- **JWT (HS256)** — подпись и валидация через `io.jsonwebtoken` (JJWT), секрет и время жизни токена конфигурируются через переменные окружения (`JWT_SECRET`, `JWT_EXPIRATION`);
- Gateway **не выпускает** токены — только проверяет подпись и срок действия, аутентификация пользователя — задача отдельного сервиса; здесь реализован чисто **resource server**-сценарий на границе системы;
- успешно прошедший проверку запрос получает заголовок `X-User-Id` — downstream-сервисы могут доверять личности пользователя без повторной валидации токена;
- CORS настроен явно на уровне Spring Security и Gateway globalcors (allowed origins/methods/headers, `allowCredentials`).

## Отказоустойчивость

Резилиенс настроен per-route через Resilience4j (`trainsCircuitBreaker`, `trainingCircuitBreaker`, `nutritionCircuitBreaker`, `notificationCircuitBreaker`, `recommendationCircuitBreaker`, `sagaCircuitBreaker`):

| Параметр | Значение |
|---|---|
| Sliding window | 10 вызовов |
| Минимум вызовов для расчёта | 5 |
| Порог открытия (failure rate) | 50% |
| Время в открытом состоянии | 10 секунд |
| Half-open: пробных вызовов | 3 |
| Timeout на вызов (TimeLimiter) | 5 секунд |

При открытой цепи запрос форвардится на `/fallback`, который отвечает `503` с тем же `correlationId` — клиент получает предсказуемый ответ, а не зависший запрос или 500-ю с трассировкой.

## Rate Limiting

`RedisRateLimiter` (Spring Cloud Gateway) с настраиваемыми `replenish-rate` / `burst-capacity` (`RATE_LIMITER_REPLENISH_RATE`, `RATE_LIMITER_BURST_CAPACITY`). Ключ для лимитирования сейчас — IP клиента (`userKeyResolver`), что легко переключить на `X-User-Id` после JWT-фильтра без изменения остальной конфигурации.

## Observability

| Что | Как |
|---|---|
| Метрики | `/actuator/prometheus`, `/actuator/gateway` (состояние маршрутов) |
| Distributed tracing | Micrometer Tracing + Zipkin/Brave, `tracing.sampling.probability=1.0` |
| Логи | JSON-формат через `logstash-logback-encoder`, с `traceId`/`spanId` в каждой строке |
| Дашборды | Prometheus + Grafana через `monitoring/docker-compose.monitoring.yml`, готовый дашборд `gateway-dashboard.json` |
| Health | `/actuator/health` с включёнными readiness/liveness probes для Kubernetes |

## Kubernetes

Манифесты в `k8s/`:

| Файл | Назначение |
|---|---|
| `namespace.yaml` | изолированный namespace `fitness-platform` |
| `configmap.yaml` | конфигурация без пересборки образа |
| `deployment.yaml` | Deployment с readiness/liveness-пробами |
| `service.yaml` | ClusterIP-сервис |
| `ingress.yaml` | внешний вход в кластер |
| `hpa.yaml` | автомасштабирование 2→8 реплик по CPU (70%) и памяти (75%) |
| `kustomization.yaml` | сборка манифестов через Kustomize |

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык / Framework | Java 21, Spring Boot 3.x, Spring Cloud Gateway (reactive, WebFlux) |
| Service Discovery | Netflix Eureka Client + Gateway discovery locator |
| Безопасность | Spring Security (WebFlux), JJWT (HS256) |
| Отказоустойчивость | Resilience4j (Circuit Breaker + TimeLimiter) |
| Rate Limiting | Spring Cloud Gateway RedisRateLimiter, Reactive Redis |
| Observability | Micrometer + Prometheus, Micrometer Tracing + Zipkin/Brave, Logstash JSON логи |
| CI/CD | GitHub Actions с security gates, публикация образа |
| Контейнеризация | Docker, Kubernetes (Deployment/Service/Ingress/HPA/ConfigMap), Kustomize |

## Локальный запуск

### Предварительно нужны
- Java 21, Maven (или `./mvnw`)
- Redis (для rate limiting)
- Eureka Server, доступный по `EUREKA_SERVER_URL`
- Запущенные downstream-сервисы, зарегистрированные в Eureka под именами из таблицы маршрутов

### Запуск

```bash
export EUREKA_SERVER_URL=http://localhost:8761/eureka/
export JWT_SECRET=your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
export REDIS_HOST=localhost

./mvnw clean package -DskipTests
java -jar target/API_Gateway-0.0.1-SNAPSHOT.jar
```

Сервис поднимается на порту **8075**.

| Что | Где |
|---|---|
| Health | `http://localhost:8075/actuator/health` |
| Prometheus | `http://localhost:8075/actuator/prometheus` |
| Состояние маршрутов | `http://localhost:8075/actuator/gateway/routes` |

### Monitoring stack (Prometheus + Grafana)

```bash
docker compose -f monitoring/docker-compose.monitoring.yml up -d
```

Grafana — `http://localhost:3000`, готовый дашборд Gateway уже провижинится автоматически.

### Kubernetes

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml
```

## Связанные репозитории

Часть микросервисной платформы [FitFlow](https://github.com/Maru3022/project-hub):

- [Eureka Server](https://github.com/Maru3022/Eureka-server)
- [Saga Orchestrator](https://github.com/Maru3022/Saga-Orchestrator)
- [Training Service](https://github.com/Maru3022/Training-Servive)
- [Trains Service](https://github.com/Maru3022/Trains-Service)
- [Nutrition Service](https://github.com/Maru3022/Training-Nutrition)
- [Notification Service](https://github.com/Maru3022/Training_Notification)
- [Recommendation Service](https://github.com/Maru3022/Recommendation-Service)
