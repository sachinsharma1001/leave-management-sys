# Inter-Service Communication Writeup

## Synchronous Communication

The client communicates through `api-gateway`. Gateway uses Eureka service discovery and load-balanced URIs:

- `/auth/**` -> `lb://auth-service`
- `/leaves/**` -> `lb://leave-service`
- `/notifications/**` -> `lb://notification-service`

## Authentication Propagation

1. User logs in through auth-service.
2. Auth-service returns JWT.
3. Client sends `Authorization: Bearer <token>` for protected APIs.
4. Leave-service validates the token locally using the shared JWT secret.
5. Leave-service applies authorization rules using token claims.

## Asynchronous Communication

Leave-service publishes notification events to RabbitMQ:

```text
Exchange: leave.exchange
Queue: leave.notifications
Routing key: leave.notification
```

Notification-service listens on `leave.notifications` and logs each event.

## Failure Handling

- If notification publishing fails, the leave workflow is not rolled back.
- NotificationPublisher is wrapped with Resilience4j Circuit Breaker.
- Fallback logs the event and error.

## Assumptions

- Since notification is log-based, notification failure should not fail leave application/approval/rejection.
- For production, in-memory data should be replaced with PostgreSQL/MySQL and idempotency should be introduced for RabbitMQ consumers.
