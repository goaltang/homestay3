package com.homestay3.homestaybackend.model.converter;

import com.homestay3.homestaybackend.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationTypeConverterTest {

    private final NotificationTypeConverter converter = new NotificationTypeConverter();

    @Test
    void convertsKnownNotificationType() {
        assertEquals(NotificationType.NEW_MESSAGE, converter.convertToEntityAttribute("NEW_MESSAGE"));
    }

    @Test
    void convertsUnknownNotificationTypeToUnknown() {
        assertEquals(NotificationType.UNKNOWN, converter.convertToEntityAttribute("LEGACY_ORDER_PAID"));
    }

    @Test
    void convertsBlankNotificationTypeToUnknown() {
        assertEquals(NotificationType.UNKNOWN, converter.convertToEntityAttribute(" "));
    }
}
