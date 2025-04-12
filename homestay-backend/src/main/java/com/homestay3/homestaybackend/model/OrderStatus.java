package com.homestay3.homestaybackend.model;

public enum OrderStatus {
    PENDING("待确认"),
    CONFIRMED("已确认"),
    CHECKED_IN("已入住"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    PAID("已支付"),
    REJECTED("已拒绝");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 