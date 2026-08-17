# SpareLink Payment Service

[![CI](https://github.com/tadiwanashe-mashongwa/payment-service/actions/workflows/ci.yml/badge.svg)](https://github.com/tadiwanashe-mashongwa/payment-service/actions/workflows/ci.yml)
[![JaCoCo coverage](https://github.com/tadiwanashe-mashongwa/payment-service/raw/main/.github/badges/jacoco.svg)](https://github.com/tadiwanashe-mashongwa/payment-service/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)](https://spring.io/projects/spring-boot)

The Payment Service owns the payment lifecycle for SpareLink orders. It creates one payment per order, publishes payment status changes, and never shares its database with another service.

## Responsibilities

- Consumes `order-created` events from Kafka and creates a `PENDING` payment.
- Enforces one payment per order at both service and database levels.
- Manages valid payment transitions: `PENDING` to `SUCCESS` or `FAILED`, and `SUCCESS` to `REFUNDED`.
- Reliably publishes `payment-status-changed` through a transactional outbox relay.
- Secures payment APIs with Keycloak-issued JWTs.

## Stack

Java 17, Spring Boot 3.5.5, PostgreSQL, JPA/Hibernate, Flyway, Kafka, Spring Security OAuth2 Resource Server, OpenAPI, Testcontainers, and Docker.

## Architecture

```mermaid
flowchart LR
    Client[Web / Mobile Client] -->|JWT| Payment[payment-service]
    Keycloak[Keycloak] -->|Issues JWT| Client
    Payment -->|Validate JWT| Keycloak
    Kafka[(Kafka)] -->|order-created| Payment
    Payment -->|Outbox relay| Kafka
    Payment -->|JPA + Flyway| Postgres[(PostgreSQL)]
    Kafka -->|payment-status-changed| Order[order-service]
    Kafka -->|payment-status-changed| Inventory[inventory-service]
```

## Payment flow

```mermaid
sequenceDiagram
    participant K as Kafka
    participant P as payment-service
    participant DB as PostgreSQL
    participant A as Admin

    K->>P: order-created
    P->>DB: Create one PENDING payment per order
    A->>P: PATCH payment status (Bearer JWT)
    P->>DB: Update payment + persist outbox event
    P->>K: Publish payment-status-changed
    P->>DB: Mark event published
```

## Database model

```mermaid
erDiagram
    PAYMENTS ||--o{ PAYMENT_OUTBOX_EVENTS : records
    PAYMENTS {
        uuid id PK
        uuid order_id UK
        uuid customer_id
        decimal amount
        varchar status
        timestamp created_at
        timestamp updated_at
        bigint version
    }
    PAYMENT_OUTBOX_EVENTS {
        uuid id PK
        uuid aggregate_id
        varchar topic
        boolean published
        int attempt_count
        boolean dead_lettered
        timestamp next_attempt_at
    }
```

## Run locally

Run the complete SpareLink stack from the platform repository:

```powershell
docker compose up --build -d
```

Payment Service is exposed at `http://localhost:8084`.

| Resource | URL |
| --- | --- |
| Health | `http://localhost:8084/actuator/health` |
| OpenAPI JSON | `http://localhost:8084/v3/api-docs` |
| Swagger UI | `http://localhost:8084/swagger-ui/index.html` |

## Security

The service validates JWTs from the configured Keycloak realm. Set `KEYCLOAK_ISSUER_URI` when running outside the platform Docker network.

| Endpoint | Access |
| --- | --- |
| `GET /actuator/health`, OpenAPI and Swagger UI | Public |
| `GET /api/payments/**` | `CUSTOMER` or `ADMIN` |
| `POST`/`PATCH /api/payments/**` | `ADMIN` |

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/payments` | Create a payment |
| `GET` | `/api/payments/{paymentId}` | Get a payment |
| `GET` | `/api/payments/customer/{customerId}` | List customer payments with pagination |
| `PATCH` | `/api/payments/{paymentId}/status` | Transition payment status |
| `GET` | `/api/payment-outbox/dead-lettered` | List dead-lettered events (ADMIN) |
| `POST` | `/api/payment-outbox/{eventId}/requeue` | Requeue an event (ADMIN) |

Use Swagger UI for the generated request and response schemas.

## Events

| Direction | Topic | Event |
| --- | --- | --- |
| Consumed | `order-created` | `OrderCreatedEvent` |
| Published | `payment-status-changed` | `PaymentStatusChangedEvent` |

## Reliability: transactional outbox

```mermaid
flowchart TD
    A[Payment status transaction] --> B[Persist payment + outbox event]
    B --> C{Relay publishes to Kafka}
    C -->|Success| D[Mark published]
    C -->|Failure| E[Increment attempt count]
    E --> F{Three attempts?}
    F -->|No| G[Retry with exponential delay]
    F -->|Yes| H[Mark dead-lettered]
    H --> I[Admin can inspect or requeue]
```

## Tests

```powershell
mvn test
```

The suite covers domain transitions, service behaviour, controller and security rules, Flyway/JPA repository behaviour, Kafka consumption, and outbox publishing. Testcontainers provides real PostgreSQL and Kafka where integration coverage requires them.

The JaCoCo HTML report is generated at `target/site/jacoco/index.html`.

## Project structure

```text
src/main/java/com/example/paymentservice
├── controller      HTTP API
├── service         payment and outbox use cases
├── entity          payment domain model
├── outbox          reliable event delivery
├── consumer        order-created Kafka consumer
├── config          security configuration
└── exception       ProblemDetail error handling
```
