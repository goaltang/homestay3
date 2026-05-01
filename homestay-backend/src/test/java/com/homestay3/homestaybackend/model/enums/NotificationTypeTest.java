package com.homestay3.homestaybackend.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTypeTest {

    @Test
    void canonicalTypeMapsLegacyOrderStatusAliases() {
        assertEquals(NotificationType.PAYMENT_RECEIVED, NotificationType.PAID.canonicalType());
        assertEquals(NotificationType.ORDER_COMPLETED, NotificationType.COMPLETED.canonicalType());
        assertEquals(NotificationType.ORDER_CONFIRMED, NotificationType.CONFIRMED.canonicalType());
        assertEquals(NotificationType.REFUND_COMPLETED, NotificationType.REFUNDED.canonicalType());
        assertEquals(NotificationType.BOOKING_CANCELLED, NotificationType.CANCELLED.canonicalType());
        assertEquals(NotificationType.ORDER_CANCELLED_BY_HOST, NotificationType.CANCELLED_BY_HOST.canonicalType());
        assertEquals(NotificationType.ORDER_CANCELLED_BY_GUEST, NotificationType.CANCELLED_BY_USER.canonicalType());
        assertEquals(NotificationType.ORDER_STATUS_CHANGED, NotificationType.PENDING.canonicalType());
    }

    @Test
    void canonicalTypeKeepsDomainNotificationTypesUnchanged() {
        assertEquals(NotificationType.ORDER_CONFIRMED, NotificationType.ORDER_CONFIRMED.canonicalType());
        assertEquals(NotificationType.PAYMENT_RECEIVED, NotificationType.PAYMENT_RECEIVED.canonicalType());
        assertEquals(NotificationType.UNKNOWN, NotificationType.UNKNOWN.canonicalType());
    }

    @Test
    void isLegacyAliasOnlyForMappedLegacyTypes() {
        assertTrue(NotificationType.PAID.isLegacyAlias());
        assertFalse(NotificationType.ORDER_CONFIRMED.isLegacyAlias());
        assertFalse(NotificationType.UNKNOWN.isLegacyAlias());
    }

    @Test
    void parseFilterValueHandlesBlankAndUnknownValuesSafely() {
        assertEquals(NotificationType.PAID, NotificationType.parseFilterValue("PAID"));
        assertEquals(NotificationType.ORDER_CONFIRMED, NotificationType.parseFilterValue(" ORDER_CONFIRMED "));
        assertNull(NotificationType.parseFilterValue("LEGACY_ORDER_PAID"));
        assertNull(NotificationType.parseFilterValue(" "));
        assertNull(NotificationType.parseFilterValue(null));
    }
}
