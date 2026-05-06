package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.event.NotificationCreatedEvent;
import com.homestay3.homestaybackend.event.NotificationUnreadCountChangedEvent;
import com.homestay3.homestaybackend.service.WebSocketNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationWebSocketEventListener {

    private final WebSocketNotificationService webSocketNotificationService;

    public NotificationWebSocketEventListener(WebSocketNotificationService webSocketNotificationService) {
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        webSocketNotificationService.sendNotificationToUser(event.userId(), event.notification());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUnreadCountChanged(NotificationUnreadCountChangedEvent event) {
        webSocketNotificationService.sendUnreadCountToUser(event.userId(), event.unreadCount());
    }
}
