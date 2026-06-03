package com.assignment.notification.service;
import com.assignment.notification.dto.NotificationEvent;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class NotificationStore {

    private final Map<Long, List<NotificationEvent>> events = new ConcurrentHashMap<>();

    public void add(NotificationEvent event) {
        events.computeIfAbsent(event.getUserId(), ignored -> Collections.synchronizedList(new ArrayList<>())).add(event);
    }

    public List<NotificationEvent> findByUser(Long userId) {
        return events.getOrDefault(userId, List.of());
    }
}
