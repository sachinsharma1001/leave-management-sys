# Leave Management System - Java 21 Spring Boot Microservices

Backend-only implementation for the Microservices Assignment 2026. Demo video is intentionally skipped as requested.

This version uses Lombok to reduce boilerplate for DTOs, models, logging, builders, and constructor injection.

## Implemented Services

| Service | Port | Responsibility |
|---|---:|---|
| discovery-service | 8761 | Eureka service registry |
| api-gateway | 8080 | Single entry point and routing |
| auth-service | 8081 | Login and JWT generation |
| leave-service | 8082 | Employees, balances, leave requests, manager approval/rejection |
| notification-service | 8083 | RabbitMQ notification consumer and notification history |
| rabbitmq | 5672 / 15672 | Async notification broker |
| elasticsearch | 9200 | Central log storage |
| logstash | 12201/udp | Docker GELF log ingestion |
| kibana | 5601 | Log search and dashboards |

## Tech Stack

- Java 21
- Lombok for DTOs/models and constructor injection
- Spring Boot 3.3.5
- Spring Cloud Gateway
- Netflix Eureka
- Spring Security
- JJWT
- RabbitMQ
- ELK Stack for centralized JSON logging
- Gradle multi-project build
- Docker Compose
- In-memory stores for MVP

## Default Users

| User ID | Username | Password | Role | Manager ID |
|---:|---|---|---|---:|
| 1 | employee1 | password | EMPLOYEE | 100 |
| 2 | employee2 | password | EMPLOYEE | 100 |
| 100 | manager1 | password | MANAGER | - |

## Run Locally with Docker Compose

```bash
docker compose up --build
```

Docker builds each Spring Boot service using Gradle inside the container. No Maven command is required.

Gateway URL:

```text
http://localhost:8080
```

Eureka dashboard:

```text
http://localhost:8761
```

RabbitMQ management console:

```text
http://localhost:15672
username: guest
password: guest
```

Kibana:

```text
http://localhost:5601
```

Logs are written as JSON by each Spring Boot service, forwarded to Logstash through Docker's GELF logging driver, and indexed in Elasticsearch as:

```text
leave-management-YYYY.MM.dd
```

Create a Kibana data view for:

```text
leave-management-*
```

## Run Without Docker

Start services in this order:

```bash
gradle :discovery-service:bootRun
gradle :auth-service:bootRun
gradle :leave-service:bootRun
gradle :notification-service:bootRun
gradle :api-gateway:bootRun
```

Build all services locally with Gradle:

```bash
gradle clean bootJar
```

## API Testing Flow

### 1. Login

```http
POST http://localhost:8080/auth/api/auth/login
Content-Type: application/json

{
  "username": "employee1",
  "password": "password"
}
```

Copy `token` from response and pass it as:

```http
Authorization: Bearer <token>
```

### 2. View Leave Balance

```http
GET http://localhost:8080/leaves/api/leaves/employees/1/balances
Authorization: Bearer <employee-token>
```

### 3. Apply for Leave

```http
POST http://localhost:8080/leaves/api/leaves/applications
Authorization: Bearer <employee-token>
Content-Type: application/json

{
  "employeeId": 1,
  "leaveType": "CASUAL",
  "startDate": "2026-07-01",
  "endDate": "2026-07-02",
  "numberOfDays": 2,
  "reason": "Family function",
  "reportingManagerId": 100
}
```

### 4. Manager View Requests

```http
GET http://localhost:8080/leaves/api/leaves/manager/requests?status=PENDING
Authorization: Bearer <manager-token>
```

### 5. Approve / Reject

```http
PATCH http://localhost:8080/leaves/api/leaves/manager/requests/1/decision
Authorization: Bearer <manager-token>
Content-Type: application/json

{
  "decision": "APPROVED",
  "comments": "Approved"
}
```

### 6. View History

```http
GET http://localhost:8080/leaves/api/leaves/employees/1/history?page=0&size=10
Authorization: Bearer <employee-token>
```

## Environment Variables

| Variable | Default | Service |
|---|---|---|
| JWT_SECRET | change-this-secret-key-change-this-secret-key | auth-service, leave-service, notification-service |
| EUREKA_URL | http://localhost:8761/eureka | gateway/services |
| SPRING_RABBITMQ_HOST | localhost | leave-service, notification-service |
| APP_ENV | local | all services |
| LOGGING_LEVEL_ROOT | INFO | all services |
| LOGGING_LEVEL_APP | INFO | all services |
| MANAGEMENT_TRACING_SAMPLING_PROBABILITY | 1.0 | all services |

## Gradle Project Structure

```text
settings.gradle
build.gradle
discovery-service/build.gradle
api-gateway/build.gradle
auth-service/build.gradle
leave-service/build.gradle
notification-service/build.gradle
```

The old Maven `pom.xml` files have been removed. Dependency management is handled through the Spring Boot and Spring Cloud BOMs in Gradle.

## Source Code Notes

- JWT token contains `userId`, `role`, `sub`, `iat`, and `exp`.
- Employees can only access their own data.
- Managers can access only employees mapped to their team.
- Leave balance is deducted only after approval.
- Notifications are published to RabbitMQ and logged by notification-service.
- If RabbitMQ is unavailable, leave-service logs the notification failure without crashing the request.
- Global exception handlers return structured error responses.
- Application logs include Micrometer tracing MDC fields such as `traceId` and `spanId` when a request is being handled.

## Documents Included

- `docs/MICROSERVICES_DESIGN.md`
- `docs/API_DOCUMENTATION.md`
- `docs/INTER_SERVICE_COMMUNICATION.md`
- `postman/LeaveManagement.postman_collection.json`


## Demp Video Link

- https://www.youtube.com/watch?v=g-dCm4MBRDA