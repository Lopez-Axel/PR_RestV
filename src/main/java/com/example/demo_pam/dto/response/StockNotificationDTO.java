package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.StockNotification;

import java.time.Instant;
import java.util.UUID;

public record StockNotificationDTO(
        UUID id,
        String status,
        int attemptCount,
        int ingredientCount,
        Instant createdAt,
        Instant sentAt,
        Instant nextAttemptAt
) {

    public static StockNotificationDTO from(StockNotification notification, int ingredientCount) {
        return new StockNotificationDTO(
                notification.getId(),
                notification.getStatus().name(),
                notification.getAttemptCount(),
                ingredientCount,
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getNextAttemptAt()
        );
    }
}
