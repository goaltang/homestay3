package com.homestay3.homestaybackend.dto;

import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import com.homestay3.homestaybackend.model.notification.OrderNotificationEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationCreateCommandTest {

    @Test
    void createsOrderEventCommandFromEventType() {
        NotificationCreateCommand command = NotificationCreateCommand.orderEvent(
                1L,
                2L,
                OrderNotificationEventType.PAYMENT_RECEIVED,
                99L,
                "paid");

        assertEquals(1L, command.userId());
        assertEquals(2L, command.actorId());
        assertEquals(NotificationType.PAYMENT_RECEIVED, command.type());
        assertEquals(EntityType.ORDER, command.entityType());
        assertEquals("99", command.entityId());
        assertEquals("paid", command.content());
    }

    @Test
    void rejectsMissingUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                NotificationCreateCommand.of(null, null, NotificationType.UNKNOWN, null, null, "content"));
    }

    @Test
    void rejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () ->
                NotificationCreateCommand.of(1L, null, NotificationType.UNKNOWN, null, null, " "));
    }
}
