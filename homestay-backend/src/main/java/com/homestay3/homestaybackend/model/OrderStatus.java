package com.homestay3.homestaybackend.model;

/**
 * 订单状态枚举
 * 统一前后端的订单状态定义
 */
public enum OrderStatus {
    // 基础状态
    PENDING("待确认"),
    CONFIRMED("已确认"),
    PAID("已支付"),
    CHECKED_IN("已入住"),
    COMPLETED("已完成"),
    
    // 取消状态
    CANCELLED("已取消"),
    CANCELLED_BY_USER("用户已取消"),
    CANCELLED_BY_HOST("房东已取消"),
    CANCELLED_SYSTEM("系统已取消"),
    
    // 支付相关状态
    PAYMENT_PENDING("支付中"),
    PAYMENT_FAILED("支付失败"),
    
    // 退款相关状态
    REFUND_PENDING("退款中"),
    REFUNDED("已退款"),
    REFUND_FAILED("退款失败"),
    
    // 入住相关状态
    READY_FOR_CHECKIN("待入住"),
    
    // 拒绝状态
    REJECTED("已拒绝");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 