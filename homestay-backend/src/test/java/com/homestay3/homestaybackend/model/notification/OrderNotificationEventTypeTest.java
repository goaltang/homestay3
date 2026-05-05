package com.homestay3.homestaybackend.model.notification;

import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.enums.NotificationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderNotificationEventTypeTest {

    @Test
    void allOrderNotificationEventsUseCanonicalNotificationTypes() {
        for (OrderNotificationEventType eventType : OrderNotificationEventType.values()) {
            assertFalse(eventType.notificationType().isLegacyAlias());
            assertEquals(eventType.notificationType(), eventType.notificationType().canonicalType());
        }
    }

    @Test
    void mapsOrderStatusToNotificationEventWithoutLeakingOrderStatusNames() {
        assertEquals(OrderNotificationEventType.ORDER_CONFIRMED,
                OrderNotificationEventType.fromOrderStatus(OrderStatus.CONFIRMED));
        assertEquals(OrderNotificationEventType.PAYMENT_RECEIVED,
                OrderNotificationEventType.fromOrderStatus(OrderStatus.PAID));
        assertEquals(OrderNotificationEventType.ORDER_COMPLETED,
                OrderNotificationEventType.fromOrderStatus(OrderStatus.COMPLETED));
        assertEquals(OrderNotificationEventType.ORDER_CANCELLED_BY_HOST,
                OrderNotificationEventType.fromOrderStatus(OrderStatus.CANCELLED_BY_HOST));
        assertEquals(NotificationType.ORDER_CANCELLED_BY_HOST,
                OrderNotificationEventType.fromOrderStatus(OrderStatus.CANCELLED_BY_HOST).notificationType());
    }

    @Test
    void mapsPaymentStatusToNotificationEventWithoutLeakingPaymentStatusNames() {
        assertEquals(OrderNotificationEventType.PAYMENT_RECEIVED,
                OrderNotificationEventType.fromPaymentStatus(PaymentStatus.PAID));
        assertEquals(OrderNotificationEventType.REFUND_REQUESTED,
                OrderNotificationEventType.fromPaymentStatus(PaymentStatus.REFUND_PENDING));
        assertEquals(OrderNotificationEventType.REFUND_COMPLETED,
                OrderNotificationEventType.fromPaymentStatus(PaymentStatus.REFUNDED));
        assertEquals(OrderNotificationEventType.ORDER_STATUS_CHANGED,
                OrderNotificationEventType.fromPaymentStatus(PaymentStatus.UNPAID));
    }

    @Test
    void safelyMapsUnknownStatusNamesToGenericStatusChangedEvent() {
        assertEquals(OrderNotificationEventType.ORDER_STATUS_CHANGED,
                OrderNotificationEventType.fromOrderStatusName("FUTURE_STATUS"));
        assertEquals(OrderNotificationEventType.ORDER_STATUS_CHANGED,
                OrderNotificationEventType.fromOrderStatusName(null));
    }

    @Test
    void cancellationEventUsesSpecificCancellationEventsWhenPossible() {
        assertEquals(OrderNotificationEventType.ORDER_CANCELLED_BY_HOST,
                OrderNotificationEventType.cancellationEvent("CANCELLED_BY_HOST", false));
        assertEquals(OrderNotificationEventType.ORDER_CANCELLED_BY_GUEST,
                OrderNotificationEventType.cancellationEvent("CANCELLED_BY_USER", true));
        assertEquals(OrderNotificationEventType.ORDER_STATUS_CHANGED,
                OrderNotificationEventType.cancellationEvent("CANCELLED_SYSTEM", false));
    }
}
