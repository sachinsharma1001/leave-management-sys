package com.assignment.leave.service;

import com.assignment.leave.dto.NotificationEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.notification.exchange}")
    private String exchange;

    @Value("${app.notification.routing-key}")
    private String routingKey;

    @CircuitBreaker(name = "notificationPublisher", fallbackMethod = "fallback")
    public void publish(NotificationEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Published notification: {}", event);
    }

    public void fallback(NotificationEvent event, Throwable ex) {
        log.error("Notification publish failed. Event will be logged locally. event={}, error={}", event, ex.getMessage());
    }
}
