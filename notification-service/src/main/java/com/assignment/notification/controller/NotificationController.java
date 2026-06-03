package com.assignment.notification.controller;

import com.assignment.notification.dto.NotificationEvent;
import com.assignment.notification.service.NotificationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationStore store;

    @GetMapping("/{userId}")
    public List<NotificationEvent> byUser(@PathVariable Long userId) {
        return store.findByUser(userId);
    }
}
