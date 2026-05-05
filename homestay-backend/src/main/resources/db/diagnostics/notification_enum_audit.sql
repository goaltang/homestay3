-- Read-only diagnostics for notifications.type and notifications.entity_type.
-- This script lists values that the current backend cannot recognize, plus
-- legacy notification aliases that are still readable but should not be newly written.

SELECT
    'unknown_notification_type' AS check_name,
    n.type AS code,
    COUNT(*) AS row_count,
    MIN(n.created_at) AS first_seen_at,
    MAX(n.created_at) AS last_seen_at
FROM notifications n
LEFT JOIN (
    SELECT 'BOOKING_REQUEST' AS code UNION ALL
    SELECT 'BOOKING_ACCEPTED' UNION ALL
    SELECT 'BOOKING_REJECTED' UNION ALL
    SELECT 'BOOKING_CANCELLED' UNION ALL
    SELECT 'BOOKING_REMINDER' UNION ALL
    SELECT 'REVIEW_REMINDER' UNION ALL
    SELECT 'NEW_MESSAGE' UNION ALL
    SELECT 'NEW_REVIEW' UNION ALL
    SELECT 'REVIEW_REPLIED' UNION ALL
    SELECT 'PASSWORD_CHANGED' UNION ALL
    SELECT 'EMAIL_VERIFIED' UNION ALL
    SELECT 'HOMESTAY_APPROVED' UNION ALL
    SELECT 'HOMESTAY_REJECTED' UNION ALL
    SELECT 'HOMESTAY_SUBMITTED' UNION ALL
    SELECT 'SYSTEM_ANNOUNCEMENT' UNION ALL
    SELECT 'WELCOME_MESSAGE' UNION ALL
    SELECT 'ORDER_CONFIRMED' UNION ALL
    SELECT 'PAYMENT_RECEIVED' UNION ALL
    SELECT 'ORDER_CANCELLED_BY_HOST' UNION ALL
    SELECT 'ORDER_CANCELLED_BY_GUEST' UNION ALL
    SELECT 'ORDER_COMPLETED' UNION ALL
    SELECT 'ORDER_STATUS_CHANGED' UNION ALL
    SELECT 'REFUND_REQUESTED' UNION ALL
    SELECT 'REFUND_APPROVED' UNION ALL
    SELECT 'REFUND_REJECTED' UNION ALL
    SELECT 'REFUND_COMPLETED' UNION ALL
    SELECT 'COUPON_EXPIRING' UNION ALL
    SELECT 'COUPON_ISSUED' UNION ALL
    SELECT 'PAID' UNION ALL
    SELECT 'CANCELLED' UNION ALL
    SELECT 'CANCELLED_BY_HOST' UNION ALL
    SELECT 'CANCELLED_BY_USER' UNION ALL
    SELECT 'COMPLETED' UNION ALL
    SELECT 'CONFIRMED' UNION ALL
    SELECT 'PENDING' UNION ALL
    SELECT 'REFUNDED' UNION ALL
    SELECT 'UNKNOWN'
) allowed_type ON allowed_type.code = n.type
WHERE n.type IS NOT NULL
  AND allowed_type.code IS NULL
GROUP BY n.type

UNION ALL

SELECT
    'unknown_entity_type' AS check_name,
    n.entity_type AS code,
    COUNT(*) AS row_count,
    MIN(n.created_at) AS first_seen_at,
    MAX(n.created_at) AS last_seen_at
FROM notifications n
LEFT JOIN (
    SELECT 'BOOKING' AS code UNION ALL
    SELECT 'MESSAGE_THREAD' UNION ALL
    SELECT 'HOMESTAY' UNION ALL
    SELECT 'REVIEW' UNION ALL
    SELECT 'USER' UNION ALL
    SELECT 'SYSTEM' UNION ALL
    SELECT 'ORDER' UNION ALL
    SELECT 'COUPON' UNION ALL
    SELECT 'MESSAGE' UNION ALL
    SELECT 'UNKNOWN'
) allowed_entity_type ON allowed_entity_type.code = n.entity_type
WHERE n.entity_type IS NOT NULL
  AND allowed_entity_type.code IS NULL
GROUP BY n.entity_type

UNION ALL

SELECT
    'legacy_notification_type' AS check_name,
    n.type AS code,
    COUNT(*) AS row_count,
    MIN(n.created_at) AS first_seen_at,
    MAX(n.created_at) AS last_seen_at
FROM notifications n
WHERE n.type IN (
    'PAID',
    'CANCELLED',
    'CANCELLED_BY_HOST',
    'CANCELLED_BY_USER',
    'COMPLETED',
    'CONFIRMED',
    'PENDING',
    'REFUNDED'
)
GROUP BY n.type

UNION ALL

SELECT
    'legacy_entity_type' AS check_name,
    n.entity_type AS code,
    COUNT(*) AS row_count,
    MIN(n.created_at) AS first_seen_at,
    MAX(n.created_at) AS last_seen_at
FROM notifications n
WHERE n.entity_type = 'MESSAGE'
GROUP BY n.entity_type

UNION ALL

SELECT
    'persisted_unknown_marker' AS check_name,
    CONCAT('type=', COALESCE(n.type, '<NULL>'), ', entity_type=', COALESCE(n.entity_type, '<NULL>')) AS code,
    COUNT(*) AS row_count,
    MIN(n.created_at) AS first_seen_at,
    MAX(n.created_at) AS last_seen_at
FROM notifications n
WHERE n.type = 'UNKNOWN'
   OR n.entity_type = 'UNKNOWN'
GROUP BY n.type, n.entity_type

ORDER BY check_name, row_count DESC, code;
