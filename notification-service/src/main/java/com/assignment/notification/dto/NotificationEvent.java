package com.assignment.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long userId;
    private String title;
    private String message;
    private OffsetDateTime createdAt;
}
