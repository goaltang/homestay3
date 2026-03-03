package com.homestay3.homestaybackend.model;

/**
 * 退款状态枚举
 */
public enum RefundStatus {
    PENDING("退款中"),
    SUCCESS("退款成功"),
    FAILED("退款失败");
    
    private final String description;
    
    RefundStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
