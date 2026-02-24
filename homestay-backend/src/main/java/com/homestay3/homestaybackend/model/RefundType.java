package com.homestay3.homestaybackend.model;

/**
 * 退款类型枚举
 */
public enum RefundType {
    USER_REQUESTED("用户申请"),
    HOST_CANCELLED("房东取消"),
    ADMIN_INITIATED("管理员发起"),
    SYSTEM_AUTOMATIC("系统自动");
    
    private final String description;
    
    RefundType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 