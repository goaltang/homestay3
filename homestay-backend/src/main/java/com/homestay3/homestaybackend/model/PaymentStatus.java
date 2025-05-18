package com.homestay3.homestaybackend.model;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    UNPAID("未支付"),
    PAID("已支付"),
    PAYMENT_FAILED("支付失败"), // 可选，如果需要追踪失败尝试
    REFUND_PENDING("退款处理中"),
    REFUNDED("已退款"),
    PARTIALLY_REFUNDED("部分退款"), // 可选
    REFUND_FAILED("退款失败"); // 可选

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 