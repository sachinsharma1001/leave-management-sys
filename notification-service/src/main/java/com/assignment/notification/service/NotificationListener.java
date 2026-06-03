package com.assignment.notification.service;

import com.assignment.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationStore store;

    @RabbitListener(queues = "${app.notification.queue}")
    public void onNotification(NotificationEvent event) {
        store.add(event);
        log.info("NOTIFICATION_LOG userId={} title={} message={} createdAt={}", event.getUserId(), event.getTitle(), event.getMessage(), event.getCreatedAt());
    }
}
