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

    // 系统通知
    SYSTEM_ANNOUNCEMENT,  // 系统公告
    WELCOME_MESSAGE,      // 欢迎新用户 // (已实现)

    // --- 新增类型 (部分已通过上面的类型实现) ---
    ORDER_CONFIRMED,      // 订单被房东确认 (给用户) // (已实现)
    PAYMENT_RECEIVED,     // 收到用户付款 (给房东) // (已实现)
    ORDER_CANCELLED_BY_HOST, // 订单被房东取消 (给用户) // (逻辑包含在 BOOKING_CANCELLED 中)
    ORDER_CANCELLED_BY_GUEST, // 订单被用户取消 (给房东) // (逻辑包含在 BOOKING_CANCELLED 中)
    ORDER_COMPLETED       // 订单已完成 (给双方)
} 