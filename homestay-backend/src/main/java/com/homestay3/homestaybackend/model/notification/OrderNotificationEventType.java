package com.homestay3.homestaybackend.model.notification;

import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.enums.EntityType;
import com.homestay3.homestaybackend.model.enums.NotificationType;

public enum OrderNotificationEventType {
    BOOKING_REQUEST(NotificationType.BOOKING_REQUEST, EntityType.BOOKING),
    AUTO_CONFIRMED_HOST(NotificationType.ORDER_CONFIRMED, EntityType.BOOKING),
    AUTO_CONFIRMED_GUEST(NotificationType.ORDER_CONFIRMED, EntityType.BOOKING),
    ORDER_CONFIRMED(NotificationType.ORDER_CONFIRMED, EntityType.ORDER),
    ORDER_REJECTED(NotificationType.BOOKING_REJECTED, EntityType.ORDER),
    ORDER_CANCELLED(NotificationType.BOOKING_CANCELLED, EntityType.ORDER),
    ORDER_CANCELLED_BY_HOST(NotificationType.ORDER_CANCELLED_BY_HOST, EntityType.ORDER),
    ORDER_CANCELLED_BY_GUEST(NotificationType.ORDER_CANCELLED_BY_GUEST, EntityType.ORDER),
    ORDER_COMPLETED(NotificationType.ORDER_COMPLETED, EntityType.ORDER),
    ORDER_STATUS_CHANGED(NotificationType.ORDER_STATUS_CHANGED, EntityType.ORDER),
    PAYMENT_RECEIVED(NotificationType.PAYMENT_RECEIVED, EntityType.ORDER),
    PAYMENT_SUCCESS_GUEST(NotificationType.BOOKING_ACCEPTED, EntityType.ORDER),
    REFUND_REQUESTED(NotificationType.REFUND_REQUESTED, EntityType.ORDER),
    REFUND_REJECTED(NotificationType.REFUND_REJECTED, EntityType.ORDER),
    REFUND_COMPLETED(NotificationType.REFUND_COMPLETED, EntityType.ORDER),
    DISPUTE_RAISED(NotificationType.REFUND_REQUESTED, EntityType.ORDER),
    DISPUTE_RESOLVED_FOR_HOST(NotificationType.ORDER_STATUS_CHANGED, EntityType.ORDER),
    DISPUTE_RESOLVED_FOR_GUEST(NotificationType.REFUND_COMPLETED, EntityType.ORDER),
    BOOKING_REMINDER(NotificationType.BOOKING_REMINDER, EntityType.ORDER);

    private final NotificationType notificationType;
    private final EntityType entityType;

    OrderNotificationEventType(NotificationType notificationType, EntityType entityType) {
        if (notificationType.isLegacyAlias()) {
            throw new IllegalArgumentException("Order notification events must not use legacy notification aliases");
        }
        this.notificationType = notificationType.canonicalType();
        this.entityType = entityType;
    }

    public NotificationType notificationType() {
        return notificationType;
    }

    public EntityType entityType() {
        return entityType;
    }

    public static OrderNotificationEventType fromOrderStatus(OrderStatus status) {
        if (status == null) {
            return ORDER_STATUS_CHANGED;
        }

        return switch (status) {
            case CONFIRMED -> ORDER_CONFIRMED;
            case PAID -> PAYMENT_RECEIVED;
            case COMPLETED -> ORDER_COMPLETED;
            case CANCELLED -> ORDER_CANCELLED;
            case CANCELLED_BY_HOST -> ORDER_CANCELLED_BY_HOST;
            case CANCELLED_BY_USER -> ORDER_CANCELLED_BY_GUEST;
            case CANCELLED_SYSTEM, PAYMENT_PENDING, PAYMENT_FAILED, CHECKED_IN,
                    CHECKED_OUT, READY_FOR_CHECKIN, DISPUTE_PENDING, REJECTED,
                    PENDING, REFUND_PENDING, REFUND_FAILED -> ORDER_STATUS_CHANGED;
            case REFUNDED -> REFUND_COMPLETED;
        };
    }

    public static OrderNotificationEventType fromPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return ORDER_STATUS_CHANGED;
        }

        return switch (status) {
            case PAID -> PAYMENT_RECEIVED;
            case REFUND_PENDING -> REFUND_REQUESTED;
            case REFUNDED, PARTIALLY_REFUNDED -> REFUND_COMPLETED;
            case REFUND_FAILED, PAYMENT_FAILED, UNPAID, DISPUTED -> ORDER_STATUS_CHANGED;
        };
    }

    public static OrderNotificationEventType fromOrderStatusName(String status) {
        if (status == null || status.isBlank()) {
            return ORDER_STATUS_CHANGED;
        }

        try {
            return fromOrderStatus(OrderStatus.valueOf(status.trim()));
        } catch (IllegalArgumentException ex) {
            return ORDER_STATUS_CHANGED;
        }
    }

    public static OrderNotificationEventType cancellationEvent(String status, boolean cancelledByUser) {
        OrderNotificationEventType eventType = fromOrderStatusName(status);
        if (eventType == ORDER_CANCELLED || eventType == ORDER_CANCELLED_BY_HOST || eventType == ORDER_CANCELLED_BY_GUEST) {
            return eventType;
        }
        return cancelledByUser ? ORDER_CANCELLED_BY_GUEST : ORDER_STATUS_CHANGED;
    }
}
