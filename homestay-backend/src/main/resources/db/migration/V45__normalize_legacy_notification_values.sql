-- Normalize legacy notification values that were persisted before the
-- notification event model was separated from order status values.

UPDATE notifications
SET
    type = CASE type
        WHEN 'PAID' THEN 'PAYMENT_RECEIVED'
        WHEN 'COMPLETED' THEN 'ORDER_COMPLETED'
        WHEN 'CONFIRMED' THEN 'ORDER_CONFIRMED'
        WHEN 'REFUNDED' THEN 'REFUND_COMPLETED'
        WHEN 'CANCELLED_BY_HOST' THEN 'ORDER_CANCELLED_BY_HOST'
        WHEN 'CANCELLED_BY_USER' THEN 'ORDER_CANCELLED_BY_GUEST'
        WHEN 'CANCELLED' THEN 'BOOKING_CANCELLED'
        WHEN 'PENDING' THEN 'ORDER_STATUS_CHANGED'
        ELSE type
    END,
    updated_at = updated_at
WHERE type IN (
    'PAID',
    'COMPLETED',
    'CONFIRMED',
    'REFUNDED',
    'CANCELLED_BY_HOST',
    'CANCELLED_BY_USER',
    'CANCELLED',
    'PENDING'
);

UPDATE notifications
SET
    entity_type = 'MESSAGE_THREAD',
    updated_at = updated_at
WHERE entity_type = 'MESSAGE';
