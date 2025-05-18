package com.homestay3.homestaybackend.model.enums;

/**
 * 通知关联的实体类型枚举
 */
public enum EntityType {
    BOOKING,         // 预订
    MESSAGE_THREAD,  // 消息会话 (如果需要)
    HOMESTAY,        // 民宿房源
    REVIEW,          // 评价
    USER,            // 用户 (例如，关注的用户发布了新房源)
    SYSTEM,          // 系统实体 (用于系统公告等)
    ORDER            // 订单
} 