package com.homestay3.homestaybackend.model.enums;

/**
 * 通知类型枚举
 */
public enum NotificationType {
    // 预订相关
    BOOKING_REQUEST,      // 新的预订请求 (给房东) // (已实现)
    BOOKING_ACCEPTED,     // 预订被接受 (给房客) // (已实现)
    BOOKING_REJECTED,     // 预订被拒绝 (给房客) // (已实现)
    BOOKING_CANCELLED,    // 预订被取消 (给双方) // (已实现)
    BOOKING_REMINDER,     // 入住提醒 (给房客)
    REVIEW_REMINDER,      // 评价提醒 (给房客)

    // 消息相关
    NEW_MESSAGE,          // 收到新私信

    // 评论相关
    NEW_REVIEW,           // 收到新评价 (给房东)
    REVIEW_REPLIED,       // 评价被回复 (给房客)

    // 账号相关
    PASSWORD_CHANGED,     // 密码修改成功
    EMAIL_VERIFIED,       // 邮箱验证成功

    // 房源相关 (房东/管理员)
    HOMESTAY_APPROVED,    // 房源审核通过
    HOMESTAY_REJECTED,    // 房源审核被拒
    HOMESTAY_SUBMITTED,   // 房源提交审核



    // 系统通知
    SYSTEM_ANNOUNCEMENT,  // 系统公告
    WELCOME_MESSAGE,      // 欢迎新用户 // (已实现)

    // --- 新增类型 (部分已通过上面的类型实现) ---
    ORDER_CONFIRMED,      // 订单被房东确认 (给用户) // (已实现)
    PAYMENT_RECEIVED,     // 收到用户付款 (给房东) // (已实现)
    ORDER_CANCELLED_BY_HOST, // 订单被房东取消 (给用户) // (逻辑包含在 BOOKING_CANCELLED 中)
    ORDER_CANCELLED_BY_GUEST, // 订单被用户取消 (给房东) // (逻辑包含在 BOOKING_CANCELLED 中)
    ORDER_COMPLETED,      // 订单已完成 (给双方)
    ORDER_STATUS_CHANGED, // 添加订单状态变更通知类型
    
    // 退款相关
    REFUND_REQUESTED,     // 退款申请已提交 (给用户)
    REFUND_APPROVED,      // 退款审核通过 (给用户)
    REFUND_REJECTED,      // 退款被拒绝 (给用户)
    REFUND_COMPLETED,     // 退款已完成 (给用户)

    // 优惠券相关
    COUPON_EXPIRING,      // 优惠券即将过期提醒
    COUPON_ISSUED,        // 优惠券发放通知

    // 兼容历史数据
    @Deprecated(forRemoval = false)
    PAID,                 // 已付款 (PAYMENT_RECEIVED 的别名)
    @Deprecated(forRemoval = false)
    CANCELLED,            // 预订/订单取消 (通用)
    @Deprecated(forRemoval = false)
    CANCELLED_BY_HOST,    // 被房东取消
    @Deprecated(forRemoval = false)
    CANCELLED_BY_USER,    // 被用户取消
    @Deprecated(forRemoval = false)
    COMPLETED,            // 订单已完成
    @Deprecated(forRemoval = false)
    CONFIRMED,            // 订单已确认
    @Deprecated(forRemoval = false)
    PENDING,              // 等待处理
    @Deprecated(forRemoval = false)
    REFUNDED,             // 已退款
    UNKNOWN;              // Unknown legacy notification type

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
