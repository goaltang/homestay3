package com.homestay3.homestaybackend.model.enums;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    // 预订/订单相关
    BOOKING_REQUEST(NotificationDomain.ORDER, "预订请求"),
    BOOKING_ACCEPTED(NotificationDomain.ORDER, "预订已接受"),
    BOOKING_REJECTED(NotificationDomain.ORDER, "预订被拒绝"),
    BOOKING_CANCELLED(NotificationDomain.ORDER, "预订已取消"),
    BOOKING_REMINDER(NotificationDomain.ORDER, "入住提醒"),
    ORDER_CONFIRMED(NotificationDomain.ORDER, "订单已确认"),
    PAYMENT_RECEIVED(NotificationDomain.ORDER, "付款已收到"),
    ORDER_CANCELLED_BY_HOST(NotificationDomain.ORDER, "订单已取消"),
    ORDER_CANCELLED_BY_GUEST(NotificationDomain.ORDER, "订单已取消"),
    ORDER_COMPLETED(NotificationDomain.ORDER, "订单已完成"),
    ORDER_STATUS_CHANGED(NotificationDomain.ORDER, "订单状态更新"),

    // 退款相关（归属订单域）
    REFUND_REQUESTED(NotificationDomain.ORDER, "退款申请"),
    REFUND_APPROVED(NotificationDomain.ORDER, "退款已通过"),
    REFUND_REJECTED(NotificationDomain.ORDER, "退款被拒绝"),
    REFUND_COMPLETED(NotificationDomain.ORDER, "退款已完成"),

    // 消息相关
    NEW_MESSAGE(NotificationDomain.MESSAGE, "新消息"),

    // 评价相关
    NEW_REVIEW(NotificationDomain.REVIEW, "新评价"),
    REVIEW_REPLIED(NotificationDomain.REVIEW, "评价回复"),
    REVIEW_REMINDER(NotificationDomain.REVIEW, "评价提醒"),

    // 房源相关
    HOMESTAY_APPROVED(NotificationDomain.HOMESTAY, "房源审核通过"),
    HOMESTAY_REJECTED(NotificationDomain.HOMESTAY, "房源审核未通过"),
    HOMESTAY_SUBMITTED(NotificationDomain.HOMESTAY, "房源待审核"),

    // 优惠券相关
    COUPON_EXPIRING(NotificationDomain.COUPON, "优惠券即将过期"),
    COUPON_ISSUED(NotificationDomain.COUPON, "优惠券到账"),

    // 系统/账号相关
    SYSTEM_ANNOUNCEMENT(NotificationDomain.SYSTEM, "系统公告"),
    WELCOME_MESSAGE(NotificationDomain.SYSTEM, "欢迎消息"),
    PASSWORD_CHANGED(NotificationDomain.SYSTEM, "账号安全"),
    EMAIL_VERIFIED(NotificationDomain.SYSTEM, "账号安全"),

    // 兼容历史数据
    @Deprecated(forRemoval = false)
    PAID(NotificationDomain.ORDER, "已付款"),
    @Deprecated(forRemoval = false)
    CANCELLED(NotificationDomain.ORDER, "已取消"),
    @Deprecated(forRemoval = false)
    CANCELLED_BY_HOST(NotificationDomain.ORDER, "房东取消"),
    @Deprecated(forRemoval = false)
    CANCELLED_BY_USER(NotificationDomain.ORDER, "用户取消"),
    @Deprecated(forRemoval = false)
    COMPLETED(NotificationDomain.ORDER, "已完成"),
    @Deprecated(forRemoval = false)
    CONFIRMED(NotificationDomain.ORDER, "已确认"),
    @Deprecated(forRemoval = false)
    PENDING(NotificationDomain.ORDER, "待处理"),
    @Deprecated(forRemoval = false)
    REFUNDED(NotificationDomain.ORDER, "已退款"),
    UNKNOWN(NotificationDomain.SYSTEM, "系统通知");

    private final NotificationDomain domain;
    private final String defaultTitle;

    NotificationType(NotificationDomain domain, String defaultTitle) {
        this.domain = domain;
        this.defaultTitle = defaultTitle;
    }

    public NotificationDomain getDomain() {
        return domain;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public NotificationType canonicalType() {
        return switch (this) {
            case PAID -> PAYMENT_RECEIVED;
            case COMPLETED -> ORDER_COMPLETED;
            case CONFIRMED -> ORDER_CONFIRMED;
            case REFUNDED -> REFUND_COMPLETED;
            case CANCELLED_BY_HOST -> ORDER_CANCELLED_BY_HOST;
            case CANCELLED_BY_USER -> ORDER_CANCELLED_BY_GUEST;
            case CANCELLED -> BOOKING_CANCELLED;
            case PENDING -> ORDER_STATUS_CHANGED;
            default -> this;
        };
    }

    public boolean isLegacyAlias() {
        return canonicalType() != this;
    }

    public static NotificationType parseFilterValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return NotificationType.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
