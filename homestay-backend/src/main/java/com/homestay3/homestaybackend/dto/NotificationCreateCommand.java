package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.model.notification.OrderNotificationEventType;

public record NotificationCreateCommand(
        Long userId,
        Long actorId,
        NotificationType type,
        EntityType entityType,
        String entityId,
        String content) {

    public NotificationCreateCommand {
        if (userId == null) {
            throw new IllegalArgumentException("Notification userId must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Notification content must not be blank");
        }
    }

    public static NotificationCreateCommand of(
            Long userId,
            Long actorId,
            NotificationType type,
            EntityType entityType,
            String entityId,
            String content) {
        return new NotificationCreateCommand(userId, actorId, type, entityType, entityId, content);
    }

    public static NotificationCreateCommand orderEvent(
            Long userId,
            Long actorId,
            OrderNotificationEventType eventType,
            Long orderId,
            String content) {
        return orderEvent(userId, actorId, eventType, orderId != null ? String.valueOf(orderId) : null, content);
    }

    public static NotificationCreateCommand orderEvent(
            Long userId,
            Long actorId,
            OrderNotificationEventType eventType,
            String orderId,
            String content) {
        if (eventType == null) {
            throw new IllegalArgumentException("Order notification eventType must not be null");
        }
        return new NotificationCreateCommand(
                userId,
                actorId,
                eventType.notificationType(),
                eventType.entityType(),
                orderId,
                content);
    }
}
